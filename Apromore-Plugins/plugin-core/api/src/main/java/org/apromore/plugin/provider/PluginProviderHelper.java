package org.apromore.plugin.provider;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

public final class PluginProviderHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginProviderHelper.class);

    private PluginProviderHelper() {
    }

    public static <T> List<T> findPluginsByClass(final Class<T> clazz) {
        List<T> canoniserList = new ArrayList<T>();
        Class<?>[] classes = PluginProviderHelper.getAllClassesImplementingInterfaceUsingSpring(clazz);
        for (int i = 0; i < classes.length; i++) {
            Class<?> canoniserClass = classes[i];
            try {
                Object canoniser = canoniserClass.newInstance();
                if (clazz.isInstance(canoniser)) {
                    canoniserList.add(clazz.cast(canoniser));
                }
            } catch (InstantiationException | IllegalAccessException e) {
                LOGGER.warn("Could not instantiate "+clazz.getName()+": "+canoniserClass.getName());
            }
        }
        return canoniserList;
    }

    public static Class<?>[] getAllClassesImplementingInterfaceUsingSpring(final Class<?> clazz) {
        BeanDefinitionRegistry beanRegistry = new SimpleBeanDefinitionRegistry();
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(beanRegistry, false);

        TypeFilter typeFilter = new AssignableTypeFilter(clazz);
        scanner.addIncludeFilter(typeFilter);
        scanner.setIncludeAnnotationConfig(false);
        scanner.scan("org.apromore");
        String[] beans = beanRegistry.getBeanDefinitionNames();
        Class<?>[] classes = new Class<?>[beans.length];
        for (int i = 0; i < beans.length; i ++) {
            BeanDefinition def = beanRegistry.getBeanDefinition(beans[i]);
            try {
                classes[i] = Class.forName(def.getBeanClassName());
            } catch (ClassNotFoundException e) {
                LOGGER.warn("Could not find class: "+beans[i]);
            }
        }
        return classes;
    }

    public static boolean compareNullable(final String expectedType, final String actualType) {
        return expectedType == null ? true : expectedType.equals(actualType);
    }


}
