package mx.kenzie.grammar;

import org.junit.Test;
import org.valross.constantine.Constant;

import java.lang.constant.ConstantDesc;
import java.lang.invoke.MethodHandles;
import java.util.Optional;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class NullTest {

    @Test
    public void test() throws ReflectiveOperationException {
        Null n = Null.INSTANCE;
        Optional<? extends ConstantDesc> constantDesc = n.describeConstable();
        assertTrue(constantDesc.isPresent());
        Object o = constantDesc.get().resolveConstantDesc(MethodHandles.lookup());
        assertTrue(o instanceof Constant);
        assertSame(n, o);
    }

}