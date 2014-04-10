package io.github.furikuri.unsafe;

public class Runner {
    public static void main(String[] args) {

        final AnOtherClass anOtherClass = new AnOtherClass();
        final long addressForObject1 = UnsafeUtil.addressOf(anOtherClass) + 8;
        final SampleBaseClass sampleBaseClass = new SampleBaseClass();
        final long add = UnsafeUtil.addressOf(sampleBaseClass);
        UnsafeUtil.unsafe.putInt(add + 8, UnsafeUtil.unsafe.getInt(addressForObject1));
        System.out.println(sampleBaseClass.getClass());
        System.out.println(((AnOtherClass) (Object) sampleBaseClass).s);
    }
}
