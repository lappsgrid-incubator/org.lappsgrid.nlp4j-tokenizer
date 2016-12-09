package org.anc.lapps.nlp4jtokenizer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.Serializer;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Alexandru Mahmoud
 */
public class NLP4JTokenizerTest
{
    private NLP4JTokenizer nlp4JTokenizer;

    @Before
    public void setup()
    {
        nlp4JTokenizer = new NLP4JTokenizer();
    }

    @After
    public void cleanup()
    {
        nlp4JTokenizer = null;

    }
    @Test
    public void testMetadata()
    {
        String jsonMetadata = nlp4JTokenizer.getMetadata();
        assertNotNull("service.getMetadata() returned null", jsonMetadata);

        Data data = Serializer.parse(jsonMetadata, Data.class);
        assertNotNull("Unable to parse metadata json.", data);
        assertNotSame(data.getPayload().toString(), Discriminators.Uri.ERROR, data.getDiscriminator());

        ServiceMetadata metadata = new ServiceMetadata((Map) data.getPayload());

        assertEquals("Vendor is not correct", "http://www.lappsgrid.org", metadata.getVendor());
        assertEquals("Name is not correct", NLP4JTokenizer.class.getName(), metadata.getName());
        assertEquals("Version is not correct.","1.0.0-SNAPSHOT" , metadata.getVersion());
        assertEquals("License is not correct", Discriminators.Uri.APACHE2, metadata.getLicense());

        IOSpecification produces = metadata.getProduces();
        assertEquals("Produces encoding is not correct", "UTF-8", produces.getEncoding());
        assertEquals("Too many annotation types produced", 1, produces.getAnnotations().size());
        assertEquals("Too many output formats", 1, produces.getFormat().size());
        assertEquals("LIF not produced", Discriminators.Uri.LAPPS, produces.getFormat().get(0));

        IOSpecification requires = metadata.getRequires();
        assertEquals("Requires encoding is not correct", "UTF-8", requires.getEncoding());
        assertEquals("Requires Discriminator is not correct", Discriminators.Uri.TEXT, requires.getFormat().get(0));
    }

    @Test
    public void testErrorInput()
    {
        System.out.println("NLP4JTokenizerTest.testErrorInput");
        String message = "This is an error message";
        Data<String> data = new Data<>(Discriminators.Uri.ERROR, message);
        String json = nlp4JTokenizer.execute(data.asJson());
        assertNotNull("No JSON returned from the service", json);

        data = Serializer.parse(json, Data.class);
        assertEquals("Invalid discriminator returned", Discriminators.Uri.ERROR, data.getDiscriminator());
        assertEquals("The error message has changed.", message, data.getPayload());
    }

    @Test
    public void testInvalidDiscriminator()
    {
        System.out.println("NLP4JTokenizerTest.testInvalidDiscriminator");
        Data<String> data = new Data<>(Discriminators.Uri.QUERY, "");
        String json = nlp4JTokenizer.execute(data.asJson());
        assertNotNull("No JSON returned from the service", json);
        data = Serializer.parse(json, Data.class);
        assertEquals("Invalid discriminator returned: " + data.getDiscriminator(), Discriminators.Uri.ERROR, data.getDiscriminator());
    }

    @Test
    public void testExecute()
    {
        System.out.println("NLP4JTokenizerTest.testExecute");
        StringBuilder exampleSentence = new StringBuilder("This is an example of a sentence to be tokenized.\n");
        exampleSentence.append("The tokenizer will tokenize this sentence, in such a way that the sentence will be ");
        exampleSentence.append("tokenized at the end. :)\nI am so happy about this tokenizer :D");
        String payloadString =  exampleSentence.toString();
        Data<String> data = new Data<>(Discriminators.Uri.TEXT, payloadString);

        String response = nlp4JTokenizer.execute(data.asJson());
        System.out.println(response);
    }

}