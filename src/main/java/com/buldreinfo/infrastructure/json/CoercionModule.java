package com.buldreinfo.infrastructure.json;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.ValueInstantiators;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class CoercionModule extends SimpleModule {
	private static class BooleanCoercionDeserializer extends JsonDeserializer<Boolean> implements Serializable {
		@Serial private static final long serialVersionUID = 1L;
		@Override
		public Boolean deserialize(JsonParser p, @SuppressWarnings("unused") DeserializationContext ctxt) throws IOException {
			if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) return p.getIntValue() != 0;
			if (p.currentToken() == JsonToken.VALUE_TRUE) return true;
			if (p.currentToken() == JsonToken.VALUE_FALSE) return false;
			return false;
		}
	}

	private static class IntegerCoercionDeserializer extends JsonDeserializer<Integer> implements Serializable {
		@Serial private static final long serialVersionUID = 1L;
		@Override
		public Integer deserialize(JsonParser p, @SuppressWarnings("unused") DeserializationContext ctxt) throws IOException {
			if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) return p.getIntValue();
			if (p.currentToken() == JsonToken.VALUE_STRING) {
				String str = p.getValueAsString();
				return (str == null || str.isEmpty()) ? 0 : Integer.parseInt(str);
			}
			return 0;
		}
	}

	private static class RecordNullHandlingInstantiator extends StdValueInstantiator {
		@Serial private static final long serialVersionUID = 1L;
		public RecordNullHandlingInstantiator(StdValueInstantiator src) { super(src); }

		@Override
		public Object createFromObjectWith(DeserializationContext ctxt, Object[] args) throws IOException {
			for (int i = 0; i < args.length; i++) {
				if (args[i] == null) {
					Class<?> paramType = _constructorArguments[i].getType().getRawClass();
					if (paramType == int.class) args[i] = 0;
					else if (paramType == long.class) args[i] = 0L;
					else if (paramType == boolean.class) args[i] = false;
				}
			}
			return super.createFromObjectWith(ctxt, args);
		}
	}

	@Serial private static final long serialVersionUID = 1L;

	public CoercionModule() {
		addDeserializer(Boolean.class, new BooleanCoercionDeserializer());
		addDeserializer(boolean.class, new BooleanCoercionDeserializer());
		addDeserializer(Integer.class, new IntegerCoercionDeserializer());
		addDeserializer(int.class, new IntegerCoercionDeserializer());
	}

	@Override
	public void setupModule(SetupContext context) {
		super.setupModule(context);
		context.addValueInstantiators(new ValueInstantiators.Base() {
			@Override
			public ValueInstantiator findValueInstantiator(@SuppressWarnings("unused") DeserializationConfig config, BeanDescription beanDesc, ValueInstantiator defaultInstantiator) {
				if (beanDesc.getBeanClass().isRecord() && defaultInstantiator instanceof StdValueInstantiator std) {
					return new RecordNullHandlingInstantiator(std);
				}
				return defaultInstantiator;
			}
		});
	}
}