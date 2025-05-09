/*
 *  Copyright (c) 2022-2024, Mybatis-Flex (fuhai999@gmail.com).
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.mybatisflex.loveqq.framework.boot.autoconfig.datasource;

import com.kfyty.loveqq.framework.core.support.Pair;
import com.mybatisflex.annotation.UseDataSource;
import com.mybatisflex.core.datasource.DataSourceKey;
import com.mybatisflex.core.util.StringUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多数据源切换拦截器。
 *
 * @author 王帅
 * @author barql
 * @author michael
 * @since 2023-06-25
 */
public class DataSourceInterceptor implements MethodInterceptor {
    /**
     * 缓存方法对应的数据源。
     */
    private final Map<Object, String> dsCache = new ConcurrentHashMap<>();

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String dsKey = getDataSourceKey(invocation.getThis(), invocation.getMethod(), invocation.getArguments());
        if (StringUtil.noText(dsKey)) {
            return invocation.proceed();
        }
        try {
            DataSourceKey.use(dsKey);
            return invocation.proceed();
        } finally {
            DataSourceKey.clear();
        }
    }

    private String getDataSourceKey(Object target, Method method, Object[] arguments) {
        Object cacheKey = new Pair<>(method, target.getClass());
        String dsKey = this.dsCache.get(cacheKey);
        if (dsKey == null) {
            dsKey = determineDataSourceKey(method, target.getClass());
            // 对数据源取值进行动态取值处理
            if (StringUtil.hasText(dsKey)) {
                dsKey = DataSourceKey.processDataSourceKey(dsKey, target, method, arguments);
            }
            this.dsCache.put(cacheKey, dsKey);
        }
        return dsKey;
    }

    private String determineDataSourceKey(Method method, Class<?> targetClass) {
        // 方法上定义有 UseDataSource 注解
        UseDataSource annotation = method.getAnnotation(UseDataSource.class);
        if (annotation != null) {
            return annotation.value();
        }
        // 类上定义有 UseDataSource 注解
        annotation = targetClass.getAnnotation(UseDataSource.class);
        if (annotation != null) {
            return annotation.value();
        }
        // 接口上定义有 UseDataSource 注解
        Class<?>[] interfaces = targetClass.getInterfaces();
        for (Class<?> anInterface : interfaces) {
            annotation = anInterface.getAnnotation(UseDataSource.class);
            if (annotation != null) {
                return annotation.value();
            }
        }
        // 哪里都没有 UseDataSource 注解
        return "";
    }
}
