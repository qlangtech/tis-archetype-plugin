package com.qlangtech.tis.maven.plugins.archetype;

import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.apache.velocity.util.ExtProperties;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-06-06 15:47
 **/
public class TestClasspathResourceLoader extends ResourceLoader {


    @Override
    public void init(ExtProperties configuration) {

    }

    @Override
    public Reader getResourceReader(String source, String encoding) throws ResourceNotFoundException {
        InputStream res = TestClasspathResourceLoader.class.getResourceAsStream(source);
        if (res == null) {
            //  throw new IllegalStateException("res:" + source + " relevant stream source can not be null");
            return null;
        }
        try {
            return new InputStreamReader(res, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new ResourceNotFoundException(e);
        }
    }


    @Override
    public boolean isSourceModified(Resource resource) {
        return false;
    }

    @Override
    public long getLastModified(Resource resource) {
        return 0;
    }
}
