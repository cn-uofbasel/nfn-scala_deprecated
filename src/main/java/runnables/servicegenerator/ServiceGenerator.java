package runnables.servicegenerator;

import bytecode.BytecodeLoader;

public class ServiceGenerator {

    public byte[] generateService(Object clazz) {
        return BytecodeLoader.byteCodeForClass(clazz).get();
    }
}
