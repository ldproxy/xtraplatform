package de.ii.xsf.configstore.api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.io.IOException;

/**
 *
 * @author fischer
 */
@JsonTypeInfo(use=Id.CLASS, include=As.PROPERTY, property="@class")
public interface Transaction {

    void execute() throws IOException;

    void commit();

    void rollback();
    
    void close();
}
