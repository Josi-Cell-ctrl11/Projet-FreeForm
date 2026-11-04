package com.josias.floatx

/** Validate the Android Binder call layout before giving it shell identity. */
object ActivityStartCall {
    private val legacy=listOf("android.app.IApplicationThread","java.lang.String","android.content.Intent",
        "java.lang.String","android.os.IBinder","java.lang.String","int","int","android.app.ProfilerInfo","android.os.Bundle")
    private val modern=legacy.toMutableList().apply { add(2,"java.lang.String") }.toList()
    fun arguments(types:List<String>,intent:Any,options:Any):Array<Any?> {
        require(types==legacy || types==modern) { "Signature de lancement Android inconnue : ${types.joinToString()}" }
        val result=arrayOfNulls<Any>(types.size)
        result[1]="com.android.shell"
        val intentIndex=types.indexOf("android.content.Intent")
        result[intentIndex]=intent
        result[types.indexOf("android.os.Bundle")]=options
        val requestCode=types.indexOf("int")
        result[requestCode]=-1
        result[requestCode+1]=0
        return result
    }
}
