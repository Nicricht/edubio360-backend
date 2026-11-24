package cl.edubio360.auth.config;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class XmlProviderTest {
    @XmlRootElement
    public static class Example {
        public String value;
    }

    @Test
    void serializaConProveedorAlternativoSinBibliotecaVulnerable() throws Exception {
        JAXBContext context = JAXBContext.newInstance(Example.class);
        assertTrue(context.getClass().getName().startsWith("org.eclipse.persistence."));
        Example input = new Example();
        input.value = "EduBio";
        StringWriter xml = new StringWriter();
        context.createMarshaller().marshal(input, xml);
        Example output = (Example) context.createUnmarshaller().unmarshal(new StringReader(xml.toString()));
        assertEquals(input.value, output.value);
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.glassfish.jaxb.core.v2.util.XmlFactory"));
    }
}
