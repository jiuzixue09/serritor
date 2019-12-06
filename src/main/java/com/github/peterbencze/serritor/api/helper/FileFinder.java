/*
 * Copyright 2018 Peter Bencze.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.peterbencze.serritor.api.helper;

import com.github.peterbencze.serritor.api.CompleteCrawlResponse;
import com.google.common.net.InternetDomainName;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.chrome.ChromeDriver;

import java.net.URI;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Finds URLs in web element attributes located by the locating mechanisms, that match the given
 * pattern. By default, the <code>By.tagName("a")</code> locating mechanism is used and the
 * element's <code>href</code> attribute is searched for URLs.
 */
public final class FileFinder {

    private final Pattern urlPattern;
    private final Set<String> attributeTypes;
    private final String attributeName;
    private final Predicate<String> validator;
    private final int fileSize;

    private FileFinder(final ImageFinderBuilder builder) {
        urlPattern = builder.urlPattern;
        attributeTypes = builder.attributeTypes;
        attributeName = builder.attributeName;
        validator = builder.validator;
        fileSize = builder.fileSize;
    }

    /**
     * Creates a <code>UrlFinder</code> instance with the default configuration.
     *
     * @return a <code>UrlFinder</code> instance with the default configuration
     */
    public static FileFinder createDefault() {
        return new ImageFinderBuilder().build();
    }

    /**
     * Returns the pattern used for matching.
     *
     * @return the pattern used for matching
     */
    public Pattern getPattern() {
        return urlPattern;
    }


    public Set<String> getAttributeTypes() {
        return attributeTypes;
    }


    public String getAttributeName() {
        return attributeName;
    }

    /**
     * Returns the predicate used for validating URLs.
     *
     * @return the predicate used for validating URLs
     */
    public Predicate<String> getValidator() {
        return validator;
    }

    /**
     * Finds all the Files that match the pattern in the response content.
     *
     * @param response the complete crawl response
     *
     * @return all the URLs that match the pattern in the response content
     */
    public List<Map<String, Object>> findAllFiles(final CompleteCrawlResponse response) {
        Validate.notNull(response, "The response parameter cannot be null");

        Map<String, Object> map = new HashMap<>();
        Map<String, Object> stringObjectMap = ((ChromeDriver)response.getWebDriver()).executeCdpCommand("Page.getResourceTree", map);
        JSONObject json = new JSONObject(stringObjectMap);
        json = json.optJSONObject("frameTree");
//        System.out.println(json);

        return parseResources(response, json);
    }

    private List<Map<String, Object>> parseResources(CompleteCrawlResponse response, JSONObject json) {
        JSONObject frame = json.optJSONObject("frame");
        String id = frame.optString("id");
        JSONArray resources = json.optJSONArray("resources");

        List<Map<String, Object>> list = new ArrayList<>();
        resources.forEach(j -> {
            JSONObject resource = (JSONObject) j;
            Optional<Map<String, Object>> value = findInAttributeValue(response, resource, id);
            value.ifPresent(list::add);
        });
        if(json.has("childFrames")){
            JSONArray childFrames = json.optJSONArray("childFrames");
            childFrames.forEach(j ->{
                list.addAll(parseResources(response, (JSONObject) j));
            });
        }
        return list;
    }

    private Optional<Map<String, Object>> findInAttributeValue(CompleteCrawlResponse response, JSONObject resource, String id) {

        if (attributeTypes.contains(resource.optString("type"))) {
            String fileUrl = resource.optString(attributeName);
            int size = resource.optInt("contentSize");
            if(fileUrl.startsWith("data:image")) size = fileUrl.length();
            String mimeType = resource.optString("mimeType");
            if (size > fileSize) {
                Map<String, Object> m = new HashMap<>();
                m.put("frameId", id);

                try {
                    if(fileUrl.startsWith("data:image")){
                        m.put("content", fileUrl);
                        m.put("base64Encoded",true);
                    }else{
                        m.put("url", fileUrl);
                        Map<String, Object> rs = ((ChromeDriver) response.getWebDriver()).executeCdpCommand("Page.getResourceContent", m);
                        if(null != rs){
                            m.putAll(rs);
                        }
                    }
                    m.put("mimeType",mimeType);
                    return Optional.of(m);

                }catch (Exception e){
                    e.printStackTrace();
                    System.err.println(m);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Builds {@link FileFinder} instances.
     */
    public static final class ImageFinderBuilder {

        private static final Pattern DEFAULT_PATTERN = Pattern.compile("https?://\\S+");
        private static final String DEFAULT_ATTRIBUTE_TYPE = "Image";
        private static final String DEFAULT_ATTRIBUTE_NAME = "url";
        private static final Predicate<String> DEFAULT_VALIDATOR = ImageFinderBuilder::isValidUrl;
        private static final int DEFAULT_FILE_SIZE = 10000; //10k

        private Pattern urlPattern;
        private Set<String> attributeTypes;
        private String attributeName;
        private Predicate<String> validator;
        private int fileSize;

        /**
         * Creates a {@link ImageFinderBuilder} instance.
         */
        public ImageFinderBuilder() {
            urlPattern = DEFAULT_PATTERN;
            attributeTypes = Collections.singleton(DEFAULT_ATTRIBUTE_TYPE);;
            attributeName = DEFAULT_ATTRIBUTE_NAME;
            validator = DEFAULT_VALIDATOR;
            fileSize = DEFAULT_FILE_SIZE;
        }

        /**
         * Sets the pattern to use for matching.
         *
         * @param urlPattern the pattern to use for matching
         *
         * @return the <code>UrlFinderBuilder</code> instance
         */
        public ImageFinderBuilder setPattern(final Pattern urlPattern) {
            Validate.notNull(urlPattern, "The urlPattern parameter cannot be null");

            this.urlPattern = urlPattern;
            return this;
        }


        public ImageFinderBuilder setAttributeType(final String attributeType) {
            return setAttributeTypes(Collections.singleton(attributeType));
        }

        public ImageFinderBuilder setFileSize(final int fileSize) {
            Validate.notNaN(fileSize,
                    "The fileSize parameter cannot be null or empty");

            this.fileSize = fileSize;
            return this;
        }


        public ImageFinderBuilder setAttributeTypes(final Set<String> attributeTypes) {
            Validate.notEmpty(attributeTypes,
                    "The attributeTypes parameter cannot be null or empty");
            Validate.noNullElements(attributeTypes,
                    "The attributeTypes parameter cannot contain null elements");

            this.attributeTypes = attributeTypes;
            return this;
        }

        /**
         * Sets the name of the web element attribute to search for a URL.
         *
         * @param attributeName the name of the web element attribute
         *
         * @return the <code>UrlFinderBuilder</code> instance
         */
        public ImageFinderBuilder setAttributeName(final String attributeName) {
            Validate.notBlank(attributeName, "The attributeName cannot be null or blank");

            this.attributeName = attributeName;
            return this;
        }

        /**
         * Sets the predicate to use for validating URLs.
         *
         * @param validator the validator predicate
         *
         * @return the <code>UrlFinderBuilder</code> instance
         */
        public ImageFinderBuilder setValidator(final Predicate<String> validator) {
            Validate.notNull(validator, "The validator parameter cannot be null");

            this.validator = validator;
            return this;
        }

        /**
         * Builds the configured <code>UrlFinder</code> instance.
         *
         * @return the configured <code>UrlFinder</code> instance
         */
        public FileFinder build() {
            return new FileFinder(this);
        }

        /**
         * The default URL validator function.
         *
         * @param url the URL to validate
         *
         * @return <code>true</code> if the URL is valid, <code>false</code> otherwise
         */
        private static boolean isValidUrl(final String url) {
            try {
                return InternetDomainName.isValid(URI.create(url).getHost());
            } catch (IllegalArgumentException | NullPointerException exc) {
                return false;
            }
        }
    }
}
