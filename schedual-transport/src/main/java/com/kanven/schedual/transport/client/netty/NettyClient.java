package com.kanven.schedual.transport.client.netty;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.kanven.schedual.network.protoc.ResponseProto.Response;
import com.kanven.schedual.transport.client.PoolConfig;
import com.kanven.schedual.transport.client.api.Client;
import com.kanven.schedual.transport.client.api.Transform;
import com.kanven.schedual.transport.client.command.Command;

public class NettyClient<C> implements Client<C> {

	private PoolConfig config;

	private GenericObjectPool<NettyChannel> pool;

	private NettyBootstrap bootstrap = null;

	private boolean closed = false;

	public NettyClient() {

	}

	public NettyClient(PoolConfig config) {
		this.config = config;
		createPool();
	}

	public boolean isClosed() {
		return closed;
	}

	public void close() {
		if (isClosed()) {
			return;
		}
		bootstrap.close();
		pool.close();
		closed = true;
	}

	public <T> T send(Command<C> command, Transform transform) throws Exception {
		NettyChannel channel = createPool().borrowObject();
		try {
			Response response = channel.request(command.buildRequest());
			return transform.transformResponse(response);
		} finally {
			pool.returnObject(channel);
		}
	}

	private GenericObjectPool<NettyChannel> createPool() {
		if (isClosed()) {
			throw new RuntimeException("已经关闭，不可用！");
		}
		if (pool == null) {
			check();
			GenericObjectPoolConfig gpc = new GenericObjectPoolConfig();
			initConfig(gpc);
			bootstrap = new NettyBootstrap(config.getIp(), config.getPort(),
					config == null || config.getThreads() == null || config.getThreads() <= 0 ? -1
							: config.getThreads(),
					config.getConnectTimeout() == null ? PoolConfig.DEFAULT_CONNECT_TIMEOUT
							: config.getConnectTimeout());
			bootstrap.setRequestTimeout(
					config == null || config.getRequestTimeout() == null || config.getRequestTimeout() <= 0
							? PoolConfig.DEFAULT_REQUEST_TIMEOUT : config.getRequestTimeout());
			pool = new GenericObjectPool<NettyChannel>(new NettyChannelFactory(bootstrap), gpc);
			for (int i = 0; i < gpc.getMinIdle(); i++) {
				try {
					pool.addObject();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return pool;
	}

	private void check() {
		if (config == null) {
			throw new RuntimeException("客户端缺少配置信息！");
		}
		if (StringUtils.isEmpty(config.getIp())) {
			throw new RuntimeException("没有指定IP地址");
		}
		int port = config.getPort();
		if (port <= 0) {
			throw new RuntimeException("端口（" + port + "）不合法！");
		}
	}

	private void initConfig(GenericObjectPoolConfig gpc) {
		if (config != null) {
			if (config.getMinIdle() != null) {
				gpc.setMinIdle(config.getMinIdle());
			}
			if (config.getMaxIdle() != null) {
				gpc.setMaxIdle(config.getMaxIdle());
			}
			if (config.getMaxTotal() != null) {
				gpc.setMaxTotal(config.getMaxTotal());
			}
			if (config.getMaxWaitMillis() != null) {
				gpc.setMaxWaitMillis(config.getMaxWaitMillis());
			}
			if (config.getMinEvictableIdleTimeMillis() != null) {
				gpc.setMinEvictableIdleTimeMillis(config.getMinEvictableIdleTimeMillis());
			}
			if (config.getNumTestsPerEvictionRun() != null) {
				gpc.setNumTestsPerEvictionRun(config.getNumTestsPerEvictionRun());
			}
			if (config.getTestOnBorrow() != null) {
				gpc.setTestOnBorrow(config.getTestOnBorrow());
			}
			if (config.getTestOnCreate() != null) {
				gpc.setTestOnCreate(config.getTestOnCreate());
			}
			if (config.getTestOnReturn() != null) {
				gpc.setTestOnReturn(config.getTestOnReturn());
			}
			if (config.getTestWhileIdle() != null) {
				gpc.setTestWhileIdle(config.getTestWhileIdle());
			}
		}
	}

	public void setConfig(PoolConfig config) {
		this.config = config;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof NettyClient)) {
			return false;
		}
		@SuppressWarnings("unchecked")
		NettyClient<C> other = (NettyClient<C>) obj;
		if (other.config == this.config) {
			return true;
		}
		if (this.config.getIp().equals(other.config.getIp()) && this.config.getPort() == other.config.getPort()) {
			return true;
		}
		return false;
	}

}