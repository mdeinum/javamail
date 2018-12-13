package org.jsoftware.javamail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public class JavaMailJMSStatisticsTest {
    private final Address[] addressesTo;
    private JavaMailJMSStatistics javaMailJMSStatistics;
    private MimeMessage mimeMessage;

    public JavaMailJMSStatisticsTest() throws AddressException {
        addressesTo = new Address[] { new InternetAddress("nobody@nowhere.com") };
    }

    @Before
    public void setUp() throws Exception {
        javaMailJMSStatistics = new JavaMailJMSStatistics();
        javaMailJMSStatistics.registerInJMX();
        mimeMessage = Mockito.mock(MimeMessage.class);
        when(mimeMessage.getAllHeaders()).thenReturn(Collections.enumeration(Arrays.asList(new Header("h1", "v1"), new Header("h2", "v2"))));
        when(mimeMessage.getMessageID()).thenReturn("MessageId");
        when(mimeMessage.getSubject()).thenReturn("MessageSubject");
    }

    @After
    public void tearDown() {
        javaMailJMSStatistics.unregisterFromJMX();
    }

    @Test
    public void testOnSuccess() throws Exception {
        javaMailJMSStatistics.onSuccess(mimeMessage, addressesTo);
        assertEquals(1L, getMbeanAttribute("countSuccessful"));
    }

    @Test
    public void testOnFailure() throws Exception {
        javaMailJMSStatistics.onFailure(mimeMessage, addressesTo, new IllegalArgumentException());
        assertEquals(1L, getMbeanAttribute("countFailure"));
    }

    @Test
    public void testReset() throws Exception {
        Date date = new Date();
        Thread.sleep(2000);
        javaMailJMSStatistics.onSuccess(mimeMessage, addressesTo);
        javaMailJMSStatistics.invoke("reset", new Object[0], new String[0]);
        assertEquals(0L, getMbeanAttribute("countSuccessful"));
        Date d = (Date) getMbeanAttribute("statisticsCollectionStartDate");
        assertTrue(d.after(date));
    }

    @Test
    public void testJmxListener() throws Exception {
        final StringBuilder result = new StringBuilder();
        NotificationListener listener = (notification, handback) -> result.append(notification.getType());
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        platformMBeanServer.addNotificationListener(JavaMailJMSStatistics.JMX_OBJECT_NAME, listener, null, this);
        javaMailJMSStatistics.onSuccess(mimeMessage, addressesTo);
        // cleanup
        platformMBeanServer.removeNotificationListener(JavaMailJMSStatistics.JMX_OBJECT_NAME, listener);
        // check
        assertEquals(JavaMailJMSStatistics.NOTIFICATION_TYPE_SUCCESS, result.toString());
    }

    @Test
    public void testJmxMBeanInfo() throws Exception {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        MBeanInfo mBeanInfo = platformMBeanServer.getMBeanInfo(JavaMailJMSStatistics.JMX_OBJECT_NAME);
        // no exceptions = ok
        assertNotNull(mBeanInfo);
    }

    @Test
    public void testMessageInfo() throws Exception {
        javaMailJMSStatistics.onFailure(mimeMessage, addressesTo, new IllegalStateException());
        CompositeData info = (CompositeData) getMbeanAttribute("lastFailureMailInfo");
        assertTrue(info.get("date") instanceof Date);
        assertTrue(info.get("errorDescription").toString().contains("IllegalStateException"));
        TabularData tab = (TabularData) info.get("headers");
        assertEquals(2, tab.size());
        assertEquals("MessageId", info.get("messageId"));
        tab = (TabularData) info.get("toAddresses");
        assertEquals(1, tab.size());
        assertEquals("MessageSubject", info.get("subject"));
    }

    @Test
    public void testGetAttributes() throws Exception {
        List<String> attrNames = Arrays.asList("countSuccessful", "countFailure");
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        AttributeList attributeList = platformMBeanServer.getAttributes(JavaMailJMSStatistics.JMX_OBJECT_NAME, attrNames.toArray(new String[0]));
        List<Attribute> list = attributeList.asList();
        for(Attribute attribute : list) {
            if (attrNames.contains(attribute.getName())) {
                Object val = attribute.getValue();
                assertTrue(val instanceof Number);
                continue;
            }
            fail("There is more attributes then expected - " + attribute);
        }
    }

    @Test
    public void testGetAttributesEmpty() {
        AttributeList attributeList = javaMailJMSStatistics.getAttributes(new String[0]);
        assertEquals(0, attributeList.size());
    }

    @Test(expected = Exception.class)
    public void testGetAttributesNotFound() {
        javaMailJMSStatistics.getAttributes(new String[] {"NotExistingProperty"});
    }

    @Test(expected = Exception.class)
    public void testSetReadOnlyAttributes() throws Exception {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        AttributeList attributeList = new AttributeList();
        Attribute attribute = new Attribute("countSuccessful", 10L);
        attributeList.add(attribute);
        platformMBeanServer.setAttributes(JavaMailJMSStatistics.JMX_OBJECT_NAME, attributeList);
    }

    private static Object getMbeanAttribute(String attrName) throws Exception {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        return platformMBeanServer.getAttribute(JavaMailJMSStatistics.JMX_OBJECT_NAME, attrName);
    }

}