package eu.semagrow.config;

import eu.semagrow.core.config.SourceSelectorImplConfigBase;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.sail.config.ProxyRepositoryConfig;
//import eu.semagrow.commons.voidinfer.config.SEVODInferencerConfig;

/**
 * Created by angel on 11/1/14.
 */
public class RepositorySourceSelectorConfig extends SourceSelectorImplConfigBase {

    public RepositoryImplConfig getMetadataConfig() {
        /*
        SailImplConfig sailConfig = new SEVODInferencerConfig(
                                        new ForwardChainingRDFSInferencerConfig(
                                            new MemoryStoreConfig()));


        return new SailRepositoryConfig(sailConfig);
        */
        return new ProxyRepositoryConfig("semagrow_metadata");
    }

}
