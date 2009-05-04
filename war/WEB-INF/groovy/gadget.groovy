import org.codehaus.groovy.control.MultipleCompilationErrorsException
import java.io.File
import groovy.text.SimpleTemplateEngine

//def request = [ getParameter: { return "(1..3).each{println(it)}"} ] as Expando; def response = [contentType:null]

def file = new File("gadget.tmpl")
def script = request.getParameter("script")
def output = new StringWriter()
def binding = new Binding([out: new PrintWriter(output)])
def stackTrace = new StringWriter()
def errWriter = new PrintWriter(stackTrace)
def exceptionOccured = true
def result

try {
	result = new GroovyShell(binding).evaluate(script)
	exceptionOccured = false
} catch (MultipleCompilationErrorsException e) {
	stackTrace.append(e.message - 'startup failed, Script1.groovy: ')
} catch (Throwable t) {
	sanitizeStacktrace(t)
	def cause = t
	while (cause = cause?.cause) {
		sanitizeStacktrace(cause)
	}
	t.printStackTrace(errWriter)
}

def templateBinding = [script: script,
					   result: result,
					   exceptionOccured: exceptionOccured,
					   output:output.toString().replaceAll("\n",'<br>'),
					   trace: stackTrace.toString()]
def engine = new SimpleTemplateEngine()
def template = engine.createTemplate(file).make(templateBinding)

//response.contentType = "text/html; charset=utf-8"
response.contentType = "text/xml"
print template.toString()

def sanitizeStacktrace(t) {
    def filtered = [
        'com.google.', 'org.mortbay.',
        'java.', 'javax.', 'sun.', 
        'groovy.', 'org.codehaus.groovy.',
	]
    def trace = t.getStackTrace()
    def newTrace = []
    trace.each { stackTraceElement -> 
        if (filtered.every { !stackTraceElement.className.startsWith(it) }) {
            newTrace << stackTraceElement
        }
    }
    def clean = newTrace.toArray(newTrace as StackTraceElement[])
    t.stackTrace = clean
}
