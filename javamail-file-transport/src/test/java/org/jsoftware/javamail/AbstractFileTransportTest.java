package org.jsoftware.javamail;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.URLName;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

/**
 * @author szalik
 */
public class AbstractFileTransportTest {
    private final static String BASE_NAME = "base-name";
    private final static String BASE_EXT = "ext";
    private AbstractFileTransport transport;

    @Before
    public void setUp() {
        Properties properties = new Properties();
        properties.put("mail.files.path", "target" + File.separatorChar + "output");
        Session session = Session.getDefaultInstance(properties);
        transport = new AbstractFileTransport(session, new URLName("AbstractFileDev")) {
            @Override
            protected void writeMessage(Message message, OutputStream os) {
                // do nothing
            }
            @Override
            protected String getFilenameExtension() {
                return BASE_EXT;
            }
        };
        cleanup();
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        File[] files = transport.getDirectory().listFiles((dir, name) -> name.startsWith(BASE_NAME) && name.endsWith("." + BASE_EXT));
        if (files != null) {
            for(File f : files) {
                f.delete();
            }
        }
    }

    @Test
    public void testFilename() throws Exception {
        File f0 = transport.createMessageFile(BASE_NAME);
        File f1 = transport.createMessageFile(BASE_NAME);
        Assert.assertEquals("base-name.ext", f0.getName());
        Assert.assertEquals("base-name-1.ext", f1.getName());
    }
}
