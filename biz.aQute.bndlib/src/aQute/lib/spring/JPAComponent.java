package aQute.lib.spring;

import java.util.ArrayList;
import java.util.List;

import aQute.lib.osgi.Analyzer;

/**
 * This component is called when we find a resource in the META-INF/*.xml
 * pattern. We parse the resource and and the imports to the builder.
 * 
 * Parsing is done with XSLT (first time I see the use of having XML for the
 * Spring configuration files!).
 * 
 * @author aqute
 * 
 */
public class JPAComponent extends XMLTypeProcessor {
    
    protected List<XMLType> getTypes(Analyzer analyzer) throws Exception {
        List<XMLType> types = new ArrayList<XMLType>();        
        process(types,"jpa.xsl", "META-INF", "persistence.xml");
        return types;
    }
}
