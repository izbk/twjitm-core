package com.twjitm.core.common.handler.rpc;

import com.twjitm.core.common.factory.NettyRpcMethodRegistryFactory;
import com.twjitm.core.common.netstack.entity.rpc.NettyRpcRequestMessage;
import com.twjitm.core.common.netstack.entity.rpc.NettyRpcResponseMessage;
import com.twjitm.core.common.service.rpc.service.NettyRemoteRpcHandlerService;
import com.twjitm.core.spring.SpringServiceManager;
import com.twjitm.core.utils.logs.LoggerUtils;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.log4j.Logger;

import java.lang.reflect.Method;

/**
 * Created by IntelliJ IDEA.
 * User: 文江 Date: 2018/8/19  Time: 10:22
 * https://blog.csdn.net/baidu_23086307
 * <p>
 * handler rpc message
 */
public class GameNettyNetRPCServerHandler extends SimpleChannelInboundHandler<NettyRpcRequestMessage> {
    Logger logger = LoggerUtils.getLogger(GameNettyNetRPCServerHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx,final NettyRpcRequestMessage request) throws Exception {
        NettyRemoteRpcHandlerService remoteRpcHandlerService = SpringServiceManager.getSpringLoadService().getNettyRemoteRpcHandlerService();
        remoteRpcHandlerService.submit(() -> {
            if(logger.isDebugEnabled()) {
                logger.debug("RECEIVE REQUEST " + request.getRequestId());
            }
            NettyRpcResponseMessage response = new NettyRpcResponseMessage();
            response.setRequestId(request.getRequestId());
            try {
                Object result = dispatcher(request);
                response.setResult(result);
            } catch (Throwable t) {
                response.setError(t.toString());
                logger.error("RPC SERVER HANDLE REQUEST ERROR",t);
            }
            ctx.writeAndFlush(response).addListener((ChannelFutureListener) channelFuture -> {
                if(logger.isDebugEnabled()) {
                    logger.debug("SEND RESPONSE FOR REQUEST " + request.getRequestId());
                }
            });
        });
    }

    private Object dispatcher(NettyRpcRequestMessage request) throws Throwable {
        String className = request.getClassName();
        NettyRpcMethodRegistryFactory factory = SpringServiceManager.getSpringLoadService().getNettyRpcMethodRegistryFactory();
        Object serviceBean = factory.getServiceBean(className);
        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();
        if(logger.isDebugEnabled()) {
            logger.debug(methodName);
            logger.debug(serviceClass.getName());
            for (int i = 0; i < parameterTypes.length; ++i) {
                logger.debug(parameterTypes[i].getName());
            }
            for (int i = 0; i < parameters.length; ++i) {
                logger.debug(parameters[i].toString());
            }
        }
        Method method = serviceClass.getMethod(methodName, parameterTypes);
        return method.invoke(serviceBean, parameters);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        //if(logger.isErrorEnabled()) {
            logger.error("SERVER CAUGHT EXCEPTION", cause);
        //}
        ctx.close();
    }
}
