package com.dnnt.touch.netty

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.dnnt.touch.protobuf.ChatProto
import com.dnnt.touch.util.IP
import com.dnnt.touch.util.logi
import com.raizlabs.android.dbflow.kotlinextensions.async
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.protobuf.ProtobufDecoder
import io.netty.handler.codec.protobuf.ProtobufEncoder
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import java.net.InetSocketAddress

class NettyService : Service() {
    companion object {
        val TAG = "NettyService"
    }
    private val mBinder = NettyBinder()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread{
            logi(TAG,"start netty service")
            startNetty()
        }.start()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startNetty(){
        val group = NioEventLoopGroup()
        val bootstrap = Bootstrap()
        try {
            bootstrap.group(group).channel(NioSocketChannel::class.java)
                .option(ChannelOption.TCP_NODELAY,true)
                .handler(object : ChannelInitializer<SocketChannel>(){
                    override fun initChannel(ch: SocketChannel) {
                        val pl = ch.pipeline()
                        pl.addLast(ProtobufVarint32FrameDecoder())
                        pl.addLast(ProtobufDecoder(ChatProto.ChatMsg.getDefaultInstance()))
                        pl.addLast(ProtobufVarint32LengthFieldPrepender())
                        pl.addLast(ProtobufEncoder())
                        pl.addLast(MsgHandler())
                    }

                })
            val cf = bootstrap.connect(InetSocketAddress(IP,9999)).sync()
            cf.channel().closeFuture().sync()
        }finally {
            group.shutdownGracefully()
            logi(TAG,"shutdown netty thread")
        }

    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun stopService(name: Intent?): Boolean {
        logi(TAG,"stopService")
        MsgHandler.ctx.close()
        return super.stopService(name)
    }

    override fun onDestroy() {
        logi(TAG,"onDestroy")
//        if (MsgHandler.ctx.)
        super.onDestroy()
        val ctx = MsgHandler.ctx
        if (ctx.executor().isTerminated || ctx.executor().isShutdown){
            return
        }
        MsgHandler.ctx.close()
    }

    class NettyBinder : Binder(){
    }
}
