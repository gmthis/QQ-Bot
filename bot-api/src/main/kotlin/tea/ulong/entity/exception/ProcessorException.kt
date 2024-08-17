package tea.ulong.entity.exception

class ProcessorNotHaveConstructorException(message: String = "") : RuntimeException(message)

class ProcessorConstructInstanceFailedException(message: String = "") : RuntimeException(message)

class ProcessorBindException(message: String = "") : RuntimeException(message)