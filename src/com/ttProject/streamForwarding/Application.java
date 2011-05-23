package com.ttProject.streamForwarding;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.stream.BroadcastScope;
import org.red5.server.stream.IBroadcastScope;
import org.red5.server.stream.IProviderService;

public class Application extends ApplicationAdapter {
	private Map<String, BroadcastStream> streamMap = new ConcurrentHashMap<String, BroadcastStream>();
	/**
	 * on the publish start, set the listener for packet forwarding.
	 */
	@Override
	public void streamBroadcastStart(IBroadcastStream stream) {
		System.out.println("start now.");
		stream.addStreamListener(new IStreamListener() {
			@Override
			public void packetReceived(IBroadcastStream stream, IStreamPacket packet) {
				if(packet instanceof IRTMPEvent) {
					for(String key : streamMap.keySet()) {
						BroadcastStream bstream = streamMap.get(key);
						bstream.dispatchEvent((IRTMPEvent)packet);
					}
				}
			}
		});
		super.streamBroadcastStart(stream);
	}
	/**
	 * on the play start, make broadcastStream for make mirroring stream.
	 */
	@Override
	public void streamPlayItemPlay(ISubscriberStream stream, IPlayItem item,
			boolean isLive) {
		IScope scope = stream.getScope();
		String name = item.getName();
		String streamKey = scope.getContextPath() + "_" + name;
		if(!streamMap.containsKey(streamKey)) {
			// new stream.
			BroadcastStream outputStream;
			outputStream = new BroadcastStream(name);
			outputStream.setScope(scope);
			
			IProviderService service = (IProviderService)getContext().getBean(IProviderService.BEAN_NAME);
			if(service.registerBroadcastStream(scope, name, outputStream)) {
				IBroadcastScope bsScope = (BroadcastScope) service.getLiveProviderInput(scope, name, true);
				bsScope.setAttribute(IBroadcastScope.STREAM_ATTRIBUTE, outputStream);
			}
			else {
				throw new RuntimeException("Failed to make mirroring stream");
			}
			streamMap.put(streamKey, outputStream);
		}
		super.streamPlayItemPlay(stream, item, isLive);
	}
}
