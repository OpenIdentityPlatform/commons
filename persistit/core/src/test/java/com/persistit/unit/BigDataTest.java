/**
 * Copyright 2014 SonarSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.persistit.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.persistit.Exchange;
import com.persistit.Key;
import com.persistit.KeyFilter;
import com.persistit.PersistitUnitTestCase;
import com.persistit.Value;
import com.persistit.exception.PersistitException;

import org.junit.Test;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Julien HENRY
 */
public class BigDataTest extends PersistitUnitTestCase {

  public static void main(final String[] args) throws Exception {
    new BigDataTest().initAndRunTest();
  }

  @Override
  public void runAllTests() throws Exception {
    testSaveLoadBigPojo();
  }

  @Test
  public void testSaveLoadBigPojo() throws PersistitException {
    final Exchange ex = _persistit.getExchange("persistit", "BigDataTest", true);
    ex.removeAll();

    StringBuilder data = new StringBuilder();
    for (int i = 0; i < 1048575; i++) {
      data.append("a");
    }

    Pojo pojo = new Pojo();
    pojo.setId(1);
    pojo.setData(data.toString());
    pojo.setDate(new Date());

    final Key key = ex.getKey();
    key.clear().append(1);
    ex.getValue().put(pojo);
    ex.store();

    ex.fetch();
    final Pojo loadedPojo = (Pojo) ex.getValue().get();
    assertThat(loadedPojo.getData()).isEqualTo(pojo.getData());
  }

  @Test
  public void testSaveLoadVeryBigPojoNeedIncreaseMaxSize() throws PersistitException {
    final Exchange ex = _persistit.getExchange("persistit", "BigDataTest", true);
    ex.removeAll();
    ex.getValue().setMaximumSize(Value.MAXIMUM_SIZE);

    StringBuilder data = new StringBuilder();
    for (int i = 0; i < 500000; i++) {
      data.append("some data");
    }

    Pojo pojo = new Pojo();
    pojo.setId(1);
    pojo.setData(data.toString());
    pojo.setDate(new Date());

    final Key key = ex.getKey();
    key.clear().append(1);
    ex.getValue().put(pojo);
    ex.store();

    ex.fetch();
    final Pojo loadedPojo = (Pojo) ex.getValue().get();
    assertThat(loadedPojo.getData()).isEqualTo(pojo.getData());
  }

  @Test
  public void testTraverseVeryBigPojoNeedIncreaseMaxSize() throws PersistitException {
    final Exchange ex = _persistit.getExchange("persistit", "BigDataTest", true);
    ex.removeAll();
    ex.setMaximumValueSize(Value.MAXIMUM_SIZE);

    StringBuilder data = new StringBuilder();
    for (int i = 0; i < 500000; i++) {
      data.append("some data");
    }

    Pojo pojo = new Pojo();
    pojo.setId(1);
    pojo.setData(data.toString());
    pojo.setDate(new Date());

    final Key key = ex.getKey();
    key.clear().append(1);
    ex.getValue().put(pojo);
    ex.store();

    ex.clear().to(Key.BEFORE);
    KeyFilter filter = new KeyFilter().append(KeyFilter.ALL);
    Exchange itExchange = new Exchange(ex);
    assertThat(itExchange.hasNext(filter)).isTrue();

    itExchange.next(filter);

    final Pojo loadedPojo = (Pojo) ex.getValue().get();
    assertThat(loadedPojo.getData()).isEqualTo(pojo.getData());
  }

  static class Pojo implements Serializable {
    private int id;
    private String data;
    private Date date;

    public int getId() {
      return id;
    }

    public void setId(int id) {
      this.id = id;
    }

    public String getData() {
      return data;
    }

    public void setData(String data) {
      this.data = data;
    }

    public Date getDate() {
      return date;
    }

    public void setDate(Date date) {
      this.date = date;
    }

  }
}
