package com.jiuyu.governance.plugins.webmvc.serializer;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.jiuyu.framework.shandard.BaseEnum;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * 枚举接口 修改列 序列化器
 *
 * @author HeHui
 * @date 2024-12-04 15:08
 */
public class BaseEnumSerializerModifier extends BeanSerializerModifier {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BaseEnumSerializerModifier.class);
    @Serial
    private static final long serialVersionUID = -1784557527851840629L;


    /**
     * Method called by {@link BeanSerializerFactory} with tentative set
     * of discovered properties.
     * Implementations can add, remove or replace any of passed properties.
     * <p>
     * Properties <code>List</code> passed as argument is modifiable, and returned List must
     * likewise be modifiable as it may be passed to multiple registered
     * modifiers.
     * <p>
     * This method is designed to allow customization of the serialization process by modifying the set of properties to be serialized.
     * It can be used to dynamically adjust the serialization behavior based on specific conditions or configurations.
     *
     * @param config         Configuration object for serialization, providing access to configuration settings.
     * @param beanDesc       Description of the bean class, containing structural information about the class.
     * @param beanProperties List of bean properties to be serialized, which can be modified by this method.
     *
     * @return Returns the modified list of bean properties to be serialized.
     */
    @Override
    public List<BeanPropertyWriter> changeProperties(final SerializationConfig config, final BeanDescription beanDesc, final List<BeanPropertyWriter> beanProperties) {
        // Create a new list with an estimated number of elements to hold the modified properties
        List<BeanPropertyWriter> newWriters = new ArrayList<>(beanProperties.size() + 3);
        // Add the original properties to the new list
        newWriters.addAll(beanProperties);
        // Iterate through the original properties to consider possible modifications
        for (final BeanPropertyWriter writer : beanProperties) {
            boolean isBaseEnum = BaseEnum.class.isAssignableFrom(writer.getType().getRawClass());
            if (isBaseEnum) {
                // Get the name of the property
                String name = writer.getName();
                try {
                    // Create a new BeanPropertyWriter with the same name and type, but with a custom serializer
                    BeanPropertyWriter dictWrite = writer.rename(NameTransformer.simpleTransformer(null, "Desc"));
                    dictWrite.assignSerializer(new DictSerializer((Class<? extends BaseEnum<?>>) writer.getType().getRawClass()));
                    newWriters.add(dictWrite);
                } catch (Exception ignored) {
                    log.warn("[序列化] add BaseEnumSerializer error {}", name, ignored);
                    // Ignore exceptions that occur during property value retrieval
                }
            } else {
                // Check if the property has the EnumDesc annotation
                EnumDesc annotation = writer.getAnnotation(EnumDesc.class);
                if (annotation != null) {
                    // Get the name of the
                    // property
                    String name = writer.getName();
                    try {
                        BeanPropertyWriter dictWrite = writer.rename(NameTransformer.simpleTransformer(null, annotation.field()));
                        dictWrite.assignSerializer(new DictSerializer(annotation.value()));
                        newWriters.add(dictWrite);
                    } catch (Exception ignored) {
                        log.warn("[序列化] add DictSerializer error {}", name, ignored);
                        // Ignore exceptions that occur during property value retrieval
                    }
                }
            }
        }
        // Return the modified list of properties
        return newWriters;
    }

}
