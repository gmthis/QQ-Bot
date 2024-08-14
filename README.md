# 开发中

如何创建一个bot的触发词?

以下内容直接参考目标:[BasicGameplayEventProcessor](./src/main/kotlin/event/processor/BasicGameplayEventProcessor.kt)

1. 在[./src/main/kotlin/event/processor/](./src/main/kotlin/event/processor)创建类
2. 编写函数并添加[@Trigger](./src/main/kotlin/entity/event/processor/annotation/Trigger.kt)注解
3. 编写处理方法并返回String