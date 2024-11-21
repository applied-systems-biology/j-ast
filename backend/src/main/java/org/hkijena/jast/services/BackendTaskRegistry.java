package org.hkijena.jast.services;

import org.hkijena.jast.tasks.BackendTaskWorkload;
import org.hkijena.jast.tasks.BackendTaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;

@Service
public class BackendTaskRegistry {
    private final ApplicationContext applicationContext;
    private final Map<String, BackendTaskWorkload> registeredTasks = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(BackendTaskRegistry.class.getName());

    @Autowired
    public BackendTaskRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        initialize();
    }

    private void initialize() {
        Reflections reflections = new Reflections("org.hkijena.jast");
        Set<Class<?>> taskClasses = reflections.getTypesAnnotatedWith(BackendTaskType.class);
        for (Class<?> taskClass : taskClasses) {
            if(BackendTaskWorkload.class.isAssignableFrom(taskClass)) {
                try {
                    BackendTaskType annotation = taskClass.getAnnotation(BackendTaskType.class);

                    BackendTaskWorkload task = (BackendTaskWorkload) applicationContext.getBean(taskClass);
                    logger.info("Registering task {} as {}", task.getClass().getSimpleName(), annotation.typeId());
                    registeredTasks.put(annotation.typeId(), task);
                }
                catch (Exception e) {
                    logger.error("Unable to register {}: {}", taskClass, e.getMessage());
                }
            }
        }
    }

    public BackendTaskWorkload getTask(String taskName) {
        return registeredTasks.get(taskName);
    }

    public Map<String, BackendTaskWorkload> getRegisteredTasks() {
        return registeredTasks;
    }
}
