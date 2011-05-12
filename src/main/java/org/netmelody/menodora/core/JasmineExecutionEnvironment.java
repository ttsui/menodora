package org.netmelody.menodora.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.tools.shell.Global;
import org.netmelody.menodora.core.locator.Locator;

public final class JasmineExecutionEnvironment {
    
    private static final String HTML = "<html><script type=\"text/javascript\">jasmine.getEnv().execute();</script></html>";
    
    private final Context context = ContextFactory.getGlobal().enterContext();
    private final Global global = new Global();
    
    public JasmineExecutionEnvironment(boolean withDom) {
        context.setOptimizationLevel(-1);
        context.setLanguageVersion(Context.VERSION_1_5);
        global.init(context);
        
        if (withDom) {
            eval("Packages.org.mozilla.javascript.Context.getCurrentContext().setOptimizationLevel(-1);");
            loadJavaScript("/env.js-1.2/env.rhino.1.2.js");
            eval("Envjs.scriptTypes['text/javascript'] = true;");
        }
        
        loadJavaScript("/jasmine-1.0.2/jasmine.js");
    }
    
    public void executeJasmineTests(Locator javascriptResources, JasmineReporter reporter) {
        for (String resource : javascriptResources.locate()) {
            System.out.println(resource);
            loadJavaScript(resource);
        }
        
        global.put("jUnitReporter", global, reporter);
        eval("jasmine.getEnv().addReporter(jUnitReporter);");
//        eval("jasmine.getEnv().execute();");
        try {
            File loader = File.createTempFile("jasmine", ".html");
            FileUtils.writeStringToFile(loader, HTML);
            final String uri = loader.toURI().toString().replaceFirst("^file:/([^/])", "file:///$1");
            eval(String.format("window.location = '%s';", uri));
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private Object loadJavaScript(String resource) {
        try {
            URL url = getClass().getResource(resource);
            String scriptSource = IOUtils.toString(url.openStream());
            String path = url.toExternalForm();
            return context.compileString(scriptSource, path, 1, null).exec(context, global);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private Object eval(String script) {
        return context.compileString(script, "local.js", 1, null).exec(context, global);
    }
}
