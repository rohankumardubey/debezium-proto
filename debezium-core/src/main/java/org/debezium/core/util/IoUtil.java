/*
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.debezium.core.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

import org.debezium.core.annotation.Immutable;

/**
* A set of utilities for more easily performing I/O.
*/
@Immutable
public class IoUtil {

   /**
    * Read and return the entire contents of the supplied {@link InputStream stream}. This method always closes the stream when
    * finished reading.
    * 
    * @param stream the stream to the contents; may be null
    * @return the contents, or an empty byte array if the supplied reader is null
    * @throws IOException if there is an error reading the content
    */
   public static byte[] readBytes( InputStream stream ) throws IOException {
       if (stream == null) return new byte[] {};
       byte[] buffer = new byte[1024];
       try ( ByteArrayOutputStream output = new ByteArrayOutputStream() ){
           int numRead = 0;
           while ((numRead = stream.read(buffer)) > -1) {
               output.write(buffer, 0, numRead);
           }
           output.flush();
           return output.toByteArray();
       }
   }

   /**
    * Read and return the entire contents of the supplied {@link File file}.
    * 
    * @param file the file containing the contents; may be null
    * @return the contents, or an empty byte array if the supplied file is null
    * @throws IOException if there is an error reading the content
    */
   public static byte[] readBytes( File file ) throws IOException {
       if (file == null) return new byte[] {};
       try ( InputStream stream = new BufferedInputStream(new FileInputStream(file))){
           return readBytes(stream);
       }
   }

   /**
    * Read and return the entire contents of the supplied {@link Reader}. This method always closes the reader when finished
    * reading.
    * 
    * @param reader the reader of the contents; may be null
    * @return the contents, or an empty string if the supplied reader is null
    * @throws IOException if there is an error reading the content
    */
   public static String read( Reader reader ) throws IOException {
       if (reader == null) return "";
       StringBuilder sb = new StringBuilder();
       try ( Reader r = reader ){
           int numRead = 0;
           char[] buffer = new char[1024];
           while ((numRead = reader.read(buffer)) > -1) {
               sb.append(buffer, 0, numRead);
           }
       }
       return sb.toString();
   }

   /**
    * Read and return the entire contents of the supplied {@link InputStream}. This method always closes the stream when finished
    * reading.
    * 
    * @param stream the streamed contents; may be null
    * @return the contents, or an empty string if the supplied stream is null
    * @throws IOException if there is an error reading the content
    */
   public static String read( InputStream stream ) throws IOException {
       return stream == null ? "" : read(new InputStreamReader(stream));
   }

   /**
    * Read and return the entire contents of the supplied {@link InputStream}. This method always closes the stream when finished
    * reading.
    * 
    * @param stream the streamed contents; may be null
    * @param charset charset of the stream data; may not be null
    * @return the contents, or an empty string if the supplied stream is null
    * @throws IOException if there is an error reading the content
    */
   public static String read( InputStream stream,
                              String charset ) throws IOException {
       return stream == null ? "" : read(new InputStreamReader(stream, charset));
   }

   /**
    * Read and return the entire contents of the supplied {@link File}.
    * 
    * @param file the file containing the information to be read; may be null
    * @return the contents, or an empty string if the supplied reader is null
    * @throws IOException if there is an error reading the content
    */
   public static String read( File file ) throws IOException {
       if (file == null) return "";
       StringBuilder sb = new StringBuilder();
       try (Reader reader = new FileReader(file)) {
           int numRead = 0;
           char[] buffer = new char[1024];
           while ((numRead = reader.read(buffer)) > -1) {
               sb.append(buffer, 0, numRead);
           }
       }
       return sb.toString();
   }
   /**
    * Get the {@link InputStream input stream} to the resource given by the supplied path. If a class loader is supplied, the
    * method attempts to resolve the resource using the {@link ClassLoader#getResourceAsStream(String)} method; if the result is
    * non-null, it is returned. Otherwise, if a class is supplied, this method attempts to resolve the resource using the
    * {@link Class#getResourceAsStream(String)} method; if the result is non-null, it is returned. Otherwise, this method then
    * uses the Class' ClassLoader to load the resource; if non-null, it is returned . Otherwise, this method looks for an
    * existing and readable {@link File file} at the path; if found, a buffered stream to that file is returned. Otherwise, this
    * method attempts to parse the resource path into a valid {@link URL}; if this succeeds, the method attempts to open a stream
    * to that URL. If all of these fail, this method returns null.
    * 
    * @param resourcePath the logical path to the classpath, file, or URL resource
    * @param clazz the class that should be used to load the resource as a stream; may be null
    * @param classLoader the classloader that should be used to load the resource as a stream; may be null
    * @return an input stream to the resource; or null if the resource could not be found
    * @throws IllegalArgumentException if the resource path is null or empty
    */
   public static InputStream getResourceAsStream( String resourcePath,
                                                  ClassLoader classLoader,
                                                  Class<?> clazz ) {
       if ( resourcePath == null ) throw new IllegalArgumentException("resourcePath may not be null");
       InputStream result = null;
       if (classLoader != null) {
           // Try using the class loader first ...
           result = classLoader.getResourceAsStream(resourcePath);
       }
       if (result == null && clazz != null) {
           // Not yet found, so try the class ...
           result = clazz.getResourceAsStream(resourcePath);
           if (result == null) {
               // Not yet found, so try the class's class loader ...
               result = clazz.getClassLoader().getResourceAsStream(resourcePath);
           }
       }
       if (result == null) {
           // Still not found, so see if this is an existing File ...
           try {
               File file = new File(resourcePath);
               if (file.exists() && file.canRead()) {
                   return new BufferedInputStream(new FileInputStream(file));
               }
           } catch (FileNotFoundException e) {
               // just continue ...
           }
       }
       if (result == null) {
           // Still not found, so try to construct a URL out of it ...
           try {
               URL url = new URL(resourcePath);
               return url.openStream();
           } catch (MalformedURLException e) {
               // just continue ...
           } catch (IOException err) {
               // just continue ...
           }
       }
       // Couldn't find it anywhere ...
       return result;
   }

   private IoUtil() {
       // Prevent construction
   }
}
