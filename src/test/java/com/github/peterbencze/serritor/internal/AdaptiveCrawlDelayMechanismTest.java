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
package com.github.peterbencze.serritor.internal;


import java.time.Duration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openqa.selenium.JavascriptExecutor;

/**
 * Test cases for <code>AdaptiveCrawlDelayMechanism</code>.
 * 
 * @author Peter Bencze
 */
public final class AdaptiveCrawlDelayMechanismTest {
    
    private static final long LOWER_DELAY_DURATION_IN_MILLIS = Duration.ZERO.toMillis();
    private static final long MINIMUM_DELAY_DURATION_IN_MILLIS = Duration.ofSeconds(1).toMillis(); 
    private static final long IN_RANGE_DELAY_DURATION_IN_MILLIS = Duration.ofSeconds(2).toMillis();
    private static final long MAXIMUM_DELAY_DURATION_IN_MILLIS = Duration.ofSeconds(3).toMillis();
    private static final long HIGHER_DELAY_DURATION_IN_MILLIS = Duration.ofSeconds(4).toMillis();
    
    private CrawlerConfiguration mockedConfiguration;
    private JavascriptExecutor mockedJsExecutor;  
    private AdaptiveCrawlDelayMechanism crawlDelayMechanism;
    
    @Before
    public void initialize() {
        mockedConfiguration = Mockito.mock(CrawlerConfiguration.class);
        Mockito.when(mockedConfiguration.getMinimumCrawlDelayDurationInMillis())
                .thenReturn(MINIMUM_DELAY_DURATION_IN_MILLIS);  
        Mockito.when(mockedConfiguration.getMaximumCrawlDelayDurationInMillis())
                .thenReturn(MAXIMUM_DELAY_DURATION_IN_MILLIS);
        
        mockedJsExecutor = Mockito.mock(JavascriptExecutor.class);
        
        crawlDelayMechanism = new AdaptiveCrawlDelayMechanism(mockedConfiguration, mockedJsExecutor);
    }
    
    @Test
    public void testDelayLowerThanMinimum() {
        // Return a delay which is lower than the predefined minimum
        Mockito.when(mockedJsExecutor.executeScript(Mockito.anyString()))
                .thenReturn(LOWER_DELAY_DURATION_IN_MILLIS);
        
        // The minimum delay should be returned
        Assert.assertEquals(mockedConfiguration.getMinimumCrawlDelayDurationInMillis(), crawlDelayMechanism.getDelay());
    }
    
    @Test
    public void testDelayHigherThanMaximum() {
        // Return a delay which is higher than the predefined maximum
        Mockito.when(mockedJsExecutor.executeScript(Mockito.anyString()))
                .thenReturn(HIGHER_DELAY_DURATION_IN_MILLIS);
        
        // The maximum delay should be returned
        Assert.assertEquals(mockedConfiguration.getMaximumCrawlDelayDurationInMillis(), crawlDelayMechanism.getDelay());
    }
    
    @Test
    public void testDelayBetweenRange() {
        // Return an in range delay
        Mockito.when(mockedJsExecutor.executeScript(Mockito.anyString()))
                .thenReturn(IN_RANGE_DELAY_DURATION_IN_MILLIS);
        
        // The in range delay should be returned
        Assert.assertEquals(IN_RANGE_DELAY_DURATION_IN_MILLIS, crawlDelayMechanism.getDelay());
    }
}
