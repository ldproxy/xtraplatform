/*
 * Copyright 2018-2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.service.api;

/**
 * @author zahnen
 */
public class ServiceDataTest {

  /*private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDataTest.class);

  private static final ServiceData SERVICE_DATA = ImmutableServiceData.builder()
                                                                      .id("xyz")
                                                                      .createdAt(1527510249726L)
                                                                      .lastModified(1527510249726L)
                                                                      .serviceType("WFS3")
                                                                      .label("WFS 3.0 for XYZ")
                                                                      .featureProviderData(ImmutableFeatureProviderExample.builder()
                                                                                                                          .useBasicAuth(true)
                                                                                                                          .basicAuthCredentials("foo:bar")
                                                                                                                          .build())
                                                                      .build();

  private static final String SERVICE_DATA_JSON = "{\"serviceType\":\"WFS3\",\"label\":\"WFS 3.0 for XYZ\",\"shouldStart\":false,\"notifications\":[],\"featureProviderData\":{\"useBasicAuth\":true,\"basicAuthCredentials\":\"foo:bar\"},\"id\":\"xyz\",\"createdAt\":1527510249726,\"lastModified\":1527510249726}";

  private static final String SERVICE_DATA_JSON_UPDATE = "{\"label\":\"XYZ (WFS 3.0)\",\"featureProviderData\":{\"useBasicAuth\":false}}";

  private static final ServiceData SERVICE_DATA_UPDATE = ModifiableServiceData.create()
                                                                              .setLabel("XYZ (WFS 3.0)")
                                                                              .setFeatureProviderData(ModifiableFeatureProviderExample.create()
                                                                                                                                      .setUseBasicAuth(false));

  private static final ServiceData SERVICE_DATA_MERGED = ImmutableServiceData.builder()
                                                                             .from(SERVICE_DATA)
                                                                             .label(SERVICE_DATA_UPDATE.getLabel())
                                                                             .featureProviderData(ImmutableFeatureProviderExample.builder()
                                                                                                                                 .from(SERVICE_DATA.getFeatureProviderData())
                                                                                                                                 .useBasicAuth(SERVICE_DATA_UPDATE.getFeatureProviderData()
                                                                                                                                                                  .getUseBasicAuth())
                                                                                                                                 .build())
                                                                             .build();

  private static ObjectMapper objectMapper;

  @BeforeClass
  static void setup() {
      objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
                                       .registerModules(new Jdk8Module(), new GuavaModule())
      //.setDefaultMergeable(true)
      ;//.setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.SKIP));
  }

  @Test
  public void testDeSerializationFull() throws IOException {
      // full data write
      String json = objectMapper.writeValueAsString(SERVICE_DATA);

      LOGGER.info("\nTOJSON {}", json);

      Assert.assertEquals(json, SERVICE_DATA_JSON);

      // full data read
      ServiceData serviceData = objectMapper.readValue(json, ServiceData.class);

      LOGGER.info("\nTOOBJECT {} {}", serviceData, serviceData.getClass());

      Assert.assertEquals(ImmutableServiceData.builder()
                                              .from(serviceData)
                                              .build(), SERVICE_DATA);
  }

  @Test
  public void testDeSerializationPartial() throws IOException {
      ObjectMapper partialMapper = objectMapper.copy().registerModule(new SimpleModule().setSerializerModifier(new BeanSerializerModifier() {
          @Override
          public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
              return beanProperties.stream().map(bpw -> new BeanPropertyWriter(bpw) {
                  @Override
                  public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
                      if (bean.getClass().getSimpleName().startsWith("Modifiable")) {
                          if (this.getName().equals("initialized")) {
                              return;
                          }
                          try {
                              Method method = bean.getClass()
                                                  .getMethod(this.getName() + "IsSet");
                              boolean isSet = (boolean) method.invoke(bean, (Object[]) null);

                              if (isSet) {
                                  super.serializeAsField(bean, gen, prov);
                              } else {
                                  System.out.println(String.format("ignoring unset field '%s' of %s instance", this.getName(), bean.getClass().getName()));
                              }

                          } catch (Exception e) {
                              //ignore
                          }
                      } else {
                          super.serializeAsField(bean, gen, prov);
                      }
                  }
              }).collect(Collectors.toList());
          }
      }));

      // partial data write
      String json = partialMapper.writeValueAsString(SERVICE_DATA_UPDATE);

      LOGGER.info("\nTOJSON {}", json);

      Assert.assertEquals(json, SERVICE_DATA_JSON_UPDATE);

      // partial data read
      ServiceData serviceData = objectMapper.readValue(json, ServiceData.class);

      LOGGER.info("\nTOOBJECT {} {}", serviceData, serviceData.getClass());

      Assert.assertEquals(serviceData.toString(), SERVICE_DATA_UPDATE.toString());
  }

  @Test
  public void testMerging() throws IOException {
      ModifiableServiceData serviceData = ModifiableServiceData.create()
                                                               .from(SERVICE_DATA)
                                                               .setFeatureProviderData(ModifiableFeatureProviderExample.create()
                                                                                                                       .from(SERVICE_DATA.getFeatureProviderData()));

      ServiceData serviceData1 = objectMapper.readValue(SERVICE_DATA_JSON_UPDATE, ServiceData.class);
      LOGGER.info("\nMERGING \n{} \n{}", serviceData, serviceData1);

      objectMapper.copy()
                  .setDefaultMergeable(true)
                  .readerForUpdating(serviceData)
                  .readValue(SERVICE_DATA_JSON_UPDATE);

      LOGGER.info("\nMERGED {}", serviceData.toImmutable());

      Assert.assertEquals(serviceData.toImmutable(), SERVICE_DATA_MERGED);
  }*/
}
