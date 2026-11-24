package com.buldreinfo.infrastructure.json;

import java.io.Serial;
import java.io.Serializable;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.deser.ValueInstantiators;
import tools.jackson.databind.deser.std.StdValueInstantiator;
import tools.jackson.databind.module.SimpleModule;

public final class CoercionModule extends SimpleModule {
	private static class BooleanCoercionDeserializer extends ValueDeserializer<Boolean> implements Serializable {
		@Serial private static final long serialVersionUID = 1L;
		@Override
		public Boolean deserialize(JsonParser p, @SuppressWarnings("unused") DeserializationContext ctxt) throws JacksonException {
			if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) return p.getIntValue() != 0;
			if (p.currentToken() == JsonToken.VALUE_TRUE) return true;
			if (p.currentToken() == JsonToken.VALUE_FALSE) return false;
			return false;
		}
	}

	private static class IntegerCoercionDeserializer extends ValueDeserializer<Integer> implements Serializable {
		@Serial private static final long serialVersionUID = 1L;
		@Override
		public Integer deserialize(JsonParser p, @SuppressWarnings("unused") DeserializationContext ctxt) throws JacksonException {
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
		public Object createFromObjectWith(DeserializationContext ctxt, Object[] args) throws JacksonException {
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
			public ValueInstantiator modifyValueInstantiator(@SuppressWarnings("unused") DeserializationConfig config,
					BeanDescription.Supplier beanDesc, ValueInstantiator defaultInstantiator) {
				if (beanDesc.get().isRecordType() && defaultInstantiator instanceof StdValueInstantiator std) {
					return new RecordNullHandlingInstantiator(std);
				}
				return defaultInstantiator;
			}
		});
	}
}