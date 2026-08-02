Entity
=============

This seems to be a file used for caching for the Entity engine: `SunsetERP/build/resources/main/org/sitenetsoft/sunseterp/framework/entity/config/entityengine.xml`

> The path here is wrong but the XML file `entityengine.xml`.
> If you modify it then run the entity engine it overwrites the file.

This is one of the issues I ran into when trying to run the Entity engine.
```
FileLoader.getURL: fullLocation: /home/jn/Projects/Personal/SunsetERP/build/framework/entity/fieldtype/fieldtypederby.xml
01:54:17.086 [delegator-startup-1] ERROR org.sitenetsoft.sunseterp.framework.entity.model.ModelFieldTypeReader - null
org.sitenetsoft.sunseterp.framework.base.config.GenericConfigException: File Resource not found: /home/jn/Projects/Personal/SunsetERP/build/framework/entity/fieldtype/fieldtypederby.xml
        at org.sitenetsoft.sunseterp.framework.base.config.FileLoader.getURL(FileLoader.java:43) ~[main/:?]
        at org.sitenetsoft.sunseterp.framework.base.config.FileLoader.loadResource(FileLoader.java:53) ~[main/:?]
        at org.sitenetsoft.sunseterp.framework.base.config.ResourceLoader.loadResource(ResourceLoader.java:50) ~[main/:?]
        at org.sitenetsoft.sunseterp.framework.base.config.MainResourceHandler.getStream(MainResourceHandler.java:81) ~[main/:?]
        at org.sitenetsoft.sunseterp.framework.base.config.MainResourceHandler.getDocument(MainResourceHandler.java:69) ~[main/:?]
        at org.sitenetsoft.sunseterp.framework.entity.model.ModelFieldTypeReader.getModelFieldTypeReader(ModelFieldTypeReader.java:105) [main/:?]
        at org.sitenetsoft.sunseterp.framework.entity.GenericDelegator.getModelFieldTypeReader(GenericDelegator.java:585) [main/:?]
        at org.sitenetsoft.sunseterp.framework.entity.GenericDelegator.getEntityFieldType(GenericDelegator.java:570) [main/:?]
        at org.sitenetsoft.sunseterp.framework.entity.model.ModelEntityChecker.checkEntities(ModelEntityChecker.java:107) [main/:?]
        at org.sitenetsoft.sunseterp.framework.entity.GenericDelegator.<init>(GenericDelegator.java:204) [main/:?]
        at org.sitenetsoft.sunseterp.framework.entity.DelegatorFactoryImpl.getInstance(DelegatorFactoryImpl.java:36) [main/:?]
        at org.sitenetsoft.sunseterp.framework.entity.DelegatorFactoryImpl.getInstance(DelegatorFactoryImpl.java:25) [main/:?]
        at org.sitenetsoft.sunseterp.framework.base.util.UtilObject.getObjectFromFactory(UtilObject.java:123) [main/:?]
        at org.sitenetsoft.sunseterp.framework.entity.DelegatorFactory$DelegatorConfigurable.call(DelegatorFactory.java:91) [main/:?]
        at org.sitenetsoft.sunseterp.framework.entity.DelegatorFactory$DelegatorConfigurable.call(DelegatorFactory.java:81) [main/:?]
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264) [?:?]
        at java.base/java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:539) [?:?]
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264) [?:?]
        at java.base/java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:304) [?:?]
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136) [?:?]
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635) [?:?]
        at java.base/java.lang.Thread.run(Thread.java:840) [?:?]
```

