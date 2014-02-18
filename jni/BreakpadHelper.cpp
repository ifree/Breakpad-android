#include <jni.h>
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <string>
#include <vector>

#include "BreakpadHelper.h"
#include "processor/simple_symbol_supplier.h"

#define LOG(...) ((void)__android_log_print(ANDROID_LOG_WARN,  \
											 "Breakpad Helper", \
											 __VA_ARGS__))


/*extern*/ bool PrintMinidumpProcessExport(const string &minidump_file,
                                 const std::vector<string> &symbol_paths,
                                 bool machine_readable,
                                 bool ( *exists_handler	) (const string &file_name) =0,
                                 bool ( *file_read_handler	 	) (const char * file_name, char** output, size_t * readSize)=0,
                                 void ( *free_memory_handler 	) (void * pBuffer)=0
                                 );

BreakpadHelper *BreakpadHelper::mInstance=new BreakpadHelper();

BreakpadHelper *BreakpadHelper::getInstance(){
	return mInstance;
}

void BreakpadHelper::destroyInstance(){
	delete mInstance;
	mInstance=NULL;
}


bool BreakpadHelper::backtrace(const char* dump_path,
		const char* symbol_path,
		google_breakpad::SimpleSymbolSupplier::File_exists_handler exists_handler,
		google_breakpad::SimpleSymbolSupplier::File_read_handler read_handler,
		google_breakpad::SimpleSymbolSupplier::Free_memory_handler free_handler){
	this->mBacktrace.clear();
	std::vector<string> symbolpaths;
	symbolpaths.push_back(symbol_path);
	return PrintMinidumpProcessExport(dump_path,
			symbolpaths,
			false,
			exists_handler,
			read_handler,
			free_handler
			);
}

void BreakpadHelper::addBacktrace(string module,string function,string source,int line){
	BacktraceInfo backtrace;
	backtrace.module=module;
	backtrace.function=function;
	backtrace.source=source;
	backtrace.line=line;
	this->mBacktrace.push_back(backtrace);
}








