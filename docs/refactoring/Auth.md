Auth
=======

Old way:
```mermaid
flowchart Old Auth
    ServiceDispatcher.java 
    --> resources/org/sitenetsoft/sunseterp/framework/common/servicedef/services.xml 
    --> java/org/sitenetsoft/sunseterp/framework/common/login/LoginServices.java 
    --> resources/org/sitenetsoft/sunseterp/applications/party/minilang/user/UserEvents.xml
    --> java/org/sitenetsoft/sunseterp/applications/party/minilang/user/UserEvents.java
    --> java/org/sitenetsoft/sunseterp/framework/common/CommonEvents.java
    --> java/org/sitenetsoft/sunseterp/framework/webapp/control/LoginWorker.java
    --> java/org/sitenetsoft/sunseterp/framework/webapp/control/LoginEventListener.java
```

New way:
```mermaid
flowchart New Auth
    AuthResource.java --> ... --> ...
```
