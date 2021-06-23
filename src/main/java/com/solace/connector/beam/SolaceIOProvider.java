package com.solace.connector.beam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.auto.service.AutoService;
import com.solace.connector.beam.SolaceIO.InboundMessageMapper;
import com.solacesystems.jcsmp.BytesMessage;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.TextMessage;

import org.apache.beam.sdk.coders.ByteArrayCoder;
import org.apache.beam.sdk.coders.KvCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.expansion.ExternalTransformRegistrar;
import org.apache.beam.sdk.transforms.ExternalTransformBuilder;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;

public class SolaceIOProvider {
	public static class Mapper implements InboundMessageMapper<KV<String, byte[]>> {
		public byte[] getData(BytesXMLMessage msg) throws Exception {
			if (msg instanceof TextMessage) {
				return ((TextMessage) msg).getText().getBytes("UTF-8");
			} else if (msg instanceof BytesMessage) {
				return ((BytesMessage) msg).getData();
			} else {
				// we don't know what the type of message this is, but pass on the attached data
				var buffer = msg.getAttachmentByteBuffer();

				// if the buffer is backed by an array, we can use it directly
				if (buffer.hasArray()) {
					return buffer.array();
				}

				// otherwise we need to create a byte buffer
				byte[] array = new byte[buffer.remaining()];
				buffer.get(array);
				return array;
			}
		}

		@Override
		public KV<String, byte[]> map(BytesXMLMessage msg) throws Exception {
			return KV.of(msg.getDestination().getName(), getData(msg));
		}
	}

	public static class Configuration {
		private JCSMPProperties jcsmpProperties;
		private List<String> queues;
		private Boolean useSenderTimestamp;
		private Integer advanceTimeoutInMillis;
		private Long maxNumRecords;
		private Duration maxReadTime;

		public void setJcsmpProperties(Map<String, String> props) {
			this.jcsmpProperties = new JCSMPProperties();
			for (var entry : props.entrySet()) {
				this.jcsmpProperties.setProperty(entry.getKey(), entry.getValue());
			}
		}

		public void setQueues(List<String> queues) {
			this.queues = queues;
		}

		public void setUseSenderTimestamp(Boolean useSenderTimestamp) {
			this.useSenderTimestamp = useSenderTimestamp;
		}

		public void setAdvanceTimeoutInMillis(Long timeoutInMillis) {
			this.advanceTimeoutInMillis = timeoutInMillis == null ? null : timeoutInMillis.intValue();
		}

		public void setMaxNumRecords(Long maxNumRecords) {
			this.maxNumRecords = maxNumRecords;
		}

		public void setMaxReadTime(Long maxReadTime) {
			this.maxReadTime = maxReadTime == null ? null : Duration.standardSeconds(maxReadTime);
		}
	}

	public static class Builder implements ExternalTransformBuilder<Configuration, PBegin, PCollection<KV<String, byte[]>>> {
		@Override
		public PTransform<PBegin, PCollection<KV<String, byte[]>>> buildExternal(Configuration config) {
			var transform = SolaceIO.read(
				config.jcsmpProperties,
				config.queues,
				KvCoder.of(StringUtf8Coder.of(), ByteArrayCoder.of()),
				new Mapper()
			);

			if (config.useSenderTimestamp != null) {
				transform = transform.withUseSenderTimestamp(config.useSenderTimestamp);
			}

			if (config.advanceTimeoutInMillis != null) {
				transform = transform.withAdvanceTimeoutInMillis(config.advanceTimeoutInMillis);
			}

			if (config.maxNumRecords != null) {
				transform = transform.withMaxNumRecords(config.maxNumRecords);
			}

			if (config.maxReadTime != null) {
				transform = transform.withMaxReadTime(config.maxReadTime);
			}

			return transform;
		}
	}

	@AutoService(ExternalTransformRegistrar.class)
	public static class TransformRegistrar implements ExternalTransformRegistrar {
		@Override
		public Map<String, Class<? extends ExternalTransformBuilder<?, ?, ?>>> knownBuilders() {
			var map = new HashMap<String, Class<? extends ExternalTransformBuilder<?, ?, ?>>>();
			map.put("beam:external:java:solaceio:read:v1", SolaceIOProvider.Builder.class);
			return map;
		}
	}
}
