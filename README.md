# 开发中

如何创建一个bot的触发词?

以下内容直接参考目标:[BasicGameplayEventProcessor](./bot-core-plugin/src/main/kotlin/tea/ulong/event/processor/BasicGameplayEventProcessor.kt)

1. 在[./src/main/kotlin/event/processor/](./bot-api/src/main/kotlin/tea/ulong/event/processor)创建类
2. 编写函数并添加[@Trigger](./bot-api/src/main/kotlin/tea/ulong/entity/event/processor/annotation/Trigger.kt)注解
3. 编写处理方法并返回String

TODO:
1. 实现权限检查
2. 实现生命周期
3. 扩展事件支持
4. 实现外置Processor加载器
5. 实现不被消费的事件拦截
6. 实现二级触发词
7. 实现更多依赖注入
8. 其他可能的功能