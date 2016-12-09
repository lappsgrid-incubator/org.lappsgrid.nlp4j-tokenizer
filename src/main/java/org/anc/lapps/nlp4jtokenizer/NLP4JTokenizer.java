package org.anc.lapps.nlp4jtokenizer;

import edu.emory.mathcs.nlp.component.tokenizer.EnglishTokenizer;
import edu.emory.mathcs.nlp.component.tokenizer.Tokenizer;
import edu.emory.mathcs.nlp.component.tokenizer.token.Token;
import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.vocabulary.Features;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author Alexandru Mahmoud
 */

public class NLP4JTokenizer implements ProcessingService
{

    /**
     * The Json String required by getMetadata()
     */
    private String metadata;
    private static final Logger logger = LoggerFactory.getLogger(NLP4JTokenizer.class);

    public NLP4JTokenizer() { metadata = generateMetadata(); }

    private String generateMetadata()
    {
        ServiceMetadata metadata = new ServiceMetadata();
        metadata.setName(this.getClass().getName());
        metadata.setDescription("The tokenizer component of EmoryNLP's NLP4J.");
        metadata.setVersion(Version.getVersion());
        metadata.setVendor("http://www.lappsgrid.org");
        metadata.setLicense(Discriminators.Uri.APACHE2);

        IOSpecification requires = new IOSpecification();
        requires.addFormat(Discriminators.Uri.TEXT);
        requires.setEncoding("UTF-8");

        IOSpecification produces = new IOSpecification();
        produces.addFormat(Discriminators.Uri.LAPPS);
        produces.addAnnotation(Discriminators.Uri.TOKEN);
        produces.setEncoding("UTF-8");

        metadata.setRequires(requires);
        metadata.setProduces(produces);

        Data<ServiceMetadata> data = new Data<>();
        data.setDiscriminator(Discriminators.Uri.META);
        data.setPayload(metadata);
        return data.asPrettyJson();
    }

    @Override
    /**
     * Returns a JSON string containing metadata describing the service. The
     * JSON <em>must</em> conform to the json-schema at
     * <a href="http://vocab.lappsgrid.org/schema/service-schema.json">http://vocab.lappsgrid.org/schema/service-schema.json</a>
     * (processing services) or
     * <a href="http://vocab.lappsgrid.org/schema/datasource-schema.json">http://vocab.lappsgrid.org/schema/datasource-schema.json</a>
     * (datasources).
     */
    public String getMetadata() {
        return metadata;
    }

    /**
     * Entry point for a Lappsgrid service.
     * <p>
     * Each service on the Lappsgrid will accept {@code org.lappsgrid.serialization.Data} object
     * and return a {@code Data} object with a {@code org.lappsgrid.serialization.lif.Container}
     * payload.
     * <p>
     * Errors and exceptions that occur during processing should be wrapped in a {@code Data}
     * object with the discriminator set to http://vocab.lappsgrid.org/ns/error
     * <p>
     * See <a href="https://lapp.github.io/org.lappsgrid.serialization/index.html?org/lappsgrid/serialization/Data.html>org.lappsgrid.serialization.Data</a><br />
     * See <a href="https://lapp.github.io/org.lappsgrid.serialization/index.html?org/lappsgrid/serialization/lif/Container.html>org.lappsgrid.serialization.lif.Container</a><br />
     *
     * @param input A JSON string representing a Data object
     * @return A JSON string containing a Data object with a Container payload.
     */
    @Override
    public String execute(String input)
    {
        // Parse the JSON string into a Data object, and extract its discriminator.
        Data<String> data = Serializer.parse(input, Data.class);
        String discriminator = data.getDiscriminator();

        // If the Input discriminator is ERROR, return the Data as is, since it's already a wrapped error.
        if (Discriminators.Uri.ERROR.equals(discriminator))
        {
            return input;
        }

        // If the Input discriminator is not TEXT, return a wrapped Error with an appropriate message.
        else if (!Discriminators.Uri.TEXT.equals(discriminator))
        {
            String errorData = generateError("Invalid discriminator.\nExpected " + Discriminators.Uri.TEXT + "\nFound " + discriminator);
            logger.error(errorData);
            return errorData;
        }

        // Output an error if no payload is given, since an input is required to run the program
        if (data.getPayload() == null)
        {
            String errorData = generateError("No input given.");
            logger.error(errorData);
            return errorData;
        }

        // If a payload is given, process the input
        else
        {
            Tokenizer tokenizer = new EnglishTokenizer();

            String inputString = data.getPayload();
            InputStream inputStream = new ByteArrayInputStream(inputString.getBytes());
            Container container = new Container();
            container.setText(inputString);
            container.setLanguage("en");
            View view = container.newView();

            // Will track last seen position, to find index of tokens starting from that index
            int position = 0;
            // Will track last processed tokenizer, to label them with their appropriate numbers
            int tokNum = 0;

            String tokenString;
            int beginning;
            int ending;

            for (List<Token> tokens : tokenizer.segmentize(inputStream)) {
                for (Token token : tokens) {
                    tokenString = token.toString();
                    beginning = inputString.indexOf(tokenString, position);
                    ending = beginning + tokenString.length();
                    position = ending;
                    Annotation ann = view.newAnnotation("tok" + tokNum++, Discriminators.Uri.TOKEN, beginning, ending);
                    ann.addFeature(Features.Token.WORD, tokenString);
                }
            }

            try
            {
                inputStream.close();
            }

            catch(IOException e)
            {
                String errorData = generateError("Error in closing input stream.");
                logger.error(errorData);
                return errorData;
            }

            view.addContains(Discriminators.Uri.TOKEN, this.getClass().getName(), "nlp4j-tokenizer");

            Data returnData = new DataContainer(container);
            return returnData.asPrettyJson();
        }
    }


    /** This method takes an error message and returns it in a {@code Data}
     * object with the discriminator set to http://vocab.lappsgrid.org/ns/error
     *
     * @param message A string representing the error message
     * @return A JSON string containing a Data object with the message as a payload.
     */
    private String generateError(String message)
    {
        Data<String> data = new Data<>();
        data.setDiscriminator(Discriminators.Uri.ERROR);
        data.setPayload(message);
        return data.asPrettyJson();
    }

}