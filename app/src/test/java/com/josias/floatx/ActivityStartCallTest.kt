package com.josias.floatx

import org.junit.Assert.*
import org.junit.Test

class ActivityStartCallTest {
    private val legacy=listOf("android.app.IApplicationThread","java.lang.String","android.content.Intent",
        "java.lang.String","android.os.IBinder","java.lang.String","int","int","android.app.ProfilerInfo","android.os.Bundle")
    @Test fun androidLaunchLayoutsPreserveIdentityAndRouteOnlySelectedIntent() {
        val intent=Any();val options=Any()
        for(types in listOf(legacy,legacy.toMutableList().apply { add(2,"java.lang.String") })) {
            val args=ActivityStartCall.arguments(types,intent,options)
            assertNull(args[0]);assertEquals("com.android.shell",args[1])
            assertSame(intent,args[types.indexOf("android.content.Intent")])
            assertSame(options,args.last())
            assertEquals(-1,args[types.indexOf("int")]);assertEquals(0,args[types.indexOf("int")+1])
            assertEquals(5,args.count { it!=null })
        }
    }
    @Test(expected=IllegalArgumentException::class) fun unknownVendorArgumentsFailInsteadOfBeingGuessed() {
        ActivityStartCall.arguments(legacy+"boolean",Any(),Any())
    }
    @Test(expected=IllegalArgumentException::class) fun reorderedSignatureCannotLaunchWrongIntent() {
        ActivityStartCall.arguments(legacy.reversed(),Any(),Any())
    }
}
