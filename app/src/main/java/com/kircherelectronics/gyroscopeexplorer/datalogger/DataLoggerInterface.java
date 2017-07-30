package com.kircherelectronics.gyroscopeexplorer.datalogger;

/**
 * Created by KircherEngineerH on 4/27/2016.
 */
public interface DataLoggerInterface
{
    void setHeaders(Iterable<String> headers) throws IllegalStateException;
    void addRow(Iterable<String> values) throws IllegalStateException;
    String writeToFile();
}
