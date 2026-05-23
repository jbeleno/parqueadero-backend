package com.usco.parqueaderos_api.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configura el thread pool usado por los @Async (incluyendo @TransactionalEventListener
 * con @Async). Sin esta configuracion Spring usa SimpleAsyncTaskExecutor que crea un
 * thread nuevo por cada evento — bajo carga el JVM se queda sin threads.
 *
 * Tamaños conservadores: 4 core, 16 max, queue 100. Para 8 CPUs el host actual
 * tiene capacidad suficiente. Si el OCR (2.4s por imagen) se vuelve cuello de
 * botella se sube max. El nombre del bean "taskExecutor" es el que Spring usa por
 * defecto para resolver el executor; tambien resuelve el warning visto en logs:
 * "More than one TaskExecutor bean found within the context, and none is named 'taskExecutor'".
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "taskExecutor")
    @Primary
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("parq-async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("AsyncConfig: ThreadPoolTaskExecutor inicializado (core=4, max=16, queue=100)");
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }

    @Override
    public org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
