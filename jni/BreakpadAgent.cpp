/*
 * BreakpadAgent.cpp
 *
 *  Created on: 2014-2-17
 *      Author: zhangyongxiang
 */

#include <jni.h>
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <string>
#include <vector>
#include "client/linux/handler/exception_handler.h"
#include "BreakpadHelper.h"

using std::string;
using std::vector;

#define LOG(...) ((void)__android_log_print(ANDROID_LOG_WARN,  \
											 "Breakpad Agent", \
											 __VA_ARGS__))
#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

#define AGENT_CLASS "com/ifree/breakpad/BreakpadAgent"


namespace  //dump utils
{
JavaVM *GJVM;
AAssetManager *GAAssetManager;

void initAAssetManager(JNIEnv *env,jobject localThiz)
{

	if (GJVM->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
		LOG("get jni env failed");
		return;
	}
	jclass agentCls=env->GetObjectClass(localThiz);
	jmethodID jgetAssetManager=env->GetMethodID(agentCls,"getAssetManager","()Landroid/content/res/AssetManager;");
	if(jgetAssetManager == NULL)
	{
		LOG("method not exists getAssetManager");
		return;
	}
	jobject jassetManager=env->CallObjectMethod(localThiz,jgetAssetManager);
	GAAssetManager=AAssetManager_fromJava(env,jassetManager);
	LOG("init asset manager success");
}

static bool testDumpCallback(const google_breakpad::MinidumpDescriptor& descriptor,
                         void* context,
                         bool succeeded)
{
	LOG("Dump path: %s\n", descriptor.path());
  return succeeded;
}

void crash()
{
  volatile int* a = (int*)(NULL);
  *a = 1;
}

bool androidApkFileExist(const string &file_name){
	if(!GAAssetManager)
		return false;
	AAsset* asset=AAssetManager_open(GAAssetManager,file_name.c_str(),AASSET_MODE_UNKNOWN);
	LOG("check if symbol file exists: %s\n", file_name.c_str());
	if(asset == NULL)
		return false;
	AAsset_close(asset);
	LOG("symbol file exist!");
	return true;
}


bool androidApkFileRead(const char * file_name, char** output, size_t * readSize ){
	if(!GAAssetManager)
			return false;
	AAsset* asset=AAssetManager_open(GAAssetManager,file_name,AASSET_MODE_UNKNOWN);
	LOG("start read symbol file : %s\n", file_name);
		if(asset == NULL)
			return false;
	*readSize=AAsset_getLength(asset);
	//
	*output= (char*)malloc(*readSize+5);//new char[*readSize + 5];

	if(*output){
		memset(*output,0,*readSize+5);
		LOG("symbol file size : %zu\n", *readSize);
		AAsset_read(asset,*output,*readSize);
		LOG("finish read symbol data, %p",output);
		AAsset_close(asset);
		return true;
	}else
		return false;
}

void androidFreeMemory( void * memory ){
	if( memory )
		free(memory);
}
} //end namespace



//natives

void CallJava_AddFlurryParams(const char* module,const char* function,const char* source,const char* lineStr)
{
	BreakpadHelper::getInstance()->addBacktrace(module,function,source,atoi(lineStr));
}



void Native_getBacktrace(JNIEnv* env, jobject localThiz,jstring dump_path,jstring symbol_path){
	LOG("here is backtrace");
	jclass agentCls=env->GetObjectClass(localThiz);
	//I think you already know that java will do type erasure...orz, so I add this callback instead of interface callback
	jmethodID backtraceCallback=env->GetMethodID(agentCls,"execCallback","(Ljava/lang/Throwable;)V");
	if(backtraceCallback==0)
	{
		LOG("get backtrace callback failed");
		return ;
	}
	jboolean isCopy;
	const char *_dump=env->GetStringUTFChars(dump_path,&isCopy);
	const char *_symbol=env->GetStringUTFChars(symbol_path,&isCopy);
	string minidump=_dump;
	string symbol=_symbol;

	if(BreakpadHelper::getInstance()->backtrace(_dump,
			_symbol,
			&androidApkFileExist,
			&androidApkFileRead,
			&androidFreeMemory)){
		//fill stacktrace
		vector<BacktraceInfo> backtraces=*BreakpadHelper::getInstance()->getBacktraceResult();
		size_t numTraces=backtraces.size();
		string stackClsName="java/lang/StackTraceElement";
		string throwClsName="java/lang/Throwable";
		jclass stackCls=env->FindClass(stackClsName.c_str());
		jclass throwCls=env->FindClass(throwClsName.c_str());
		jmethodID stackCtor =env->GetMethodID(stackCls,"<init>","(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
		jmethodID throwCtor =env->GetMethodID(throwCls,"<init>","(Ljava/lang/String;)V");
		jmethodID addStackToThrow=env->GetMethodID(throwCls,"setStackTrace","([Ljava/lang/StackTraceElement;)V");
		if(stackCls == NULL || throwCls == NULL || stackCtor == NULL || throwCtor == NULL)
			return;
		jobjectArray jstacks = (jobjectArray) env->NewObjectArray(numTraces, stackCls, NULL);
		jobject jthrow;
		jstring jthrowTag=env->NewStringUTF("[NativeCrash]");

		for(size_t i=0;i<numTraces;i++){
			const BacktraceInfo &trace=backtraces[i];
			jstring m=env->NewStringUTF(trace.module.c_str());
			jstring f=env->NewStringUTF(trace.function.c_str());
			jstring s=env->NewStringUTF(trace.source.c_str());
			jobject jstack=env->NewObject(stackCls,stackCtor,m,f,s,trace.line);
			env->SetObjectArrayElement(jstacks,i,jstack);
			env->DeleteLocalRef(m);
			env->DeleteLocalRef(f);
			env->DeleteLocalRef(s);
			env->DeleteLocalRef(jstack);
			LOG("orz");
		}
		jthrow=env->NewObject(throwCls,throwCtor,jthrowTag);
		env->CallVoidMethod(jthrow,addStackToThrow,jstacks);//set stacktrace
		env->CallVoidMethod(localThiz,backtraceCallback,jthrow);//execute callback
		env->DeleteLocalRef(jthrow);
		env->DeleteLocalRef(jstacks);
		env->DeleteLocalRef(jthrowTag);
		backtraces.clear();
		BreakpadHelper::getInstance()->getBacktraceResult()->clear();

	}else{
		LOG("parse dump file failed");
	}
	env->ReleaseStringUTFChars(dump_path,_dump);
	env->ReleaseStringUTFChars(symbol_path,_symbol);
}

void Native_init(JNIEnv *env,jobject localThiz){
	initAAssetManager(env,localThiz);

	google_breakpad::MinidumpDescriptor descriptor("/sdcard/dump");
	google_breakpad::ExceptionHandler eh(descriptor,
	                                       NULL,
	                                       testDumpCallback,
	                                       NULL,
	                                       true,
	                                       -1);
	 //crash();

}

int registerNativeMethods(JNIEnv* env, const char *className){
	JNINativeMethod NativeCallbackMethods[] ={
			{"getBacktrace","(Ljava/lang/String;Ljava/lang/String;)V",(void*)Native_getBacktrace},
			{"nativeInit","()V",(void*)Native_init}
	};
	jclass cls;
	cls=env->FindClass(className);
	if(cls == 0){
		LOG("register native methods failed, invalid class");
	}
	if(env->RegisterNatives(cls,NativeCallbackMethods,NELEM(NativeCallbackMethods)) <0){
		LOG("register native methods failed of class %s",className);
		return -1;
	}
	return 0;
}

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    GJVM=vm;
	JNIEnv* env;
	if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
		return -1;
	}
	LOG("jni init");
	registerNativeMethods(env,AGENT_CLASS);
	return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM* vm, void * reserved)
{
    GJVM=NULL;
    LOG("jni unload");
}
