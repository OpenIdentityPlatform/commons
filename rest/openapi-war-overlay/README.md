# openapi-war-overlay

This module contains static HTML/JS/CSS for
[Swagger UI](https://github.com/swagger-api/swagger-ui), which can be included
in a Servlet-based application as a WAR overlay. All assets will be copied to
the `/src/main/webapp/openapi/` directory during a Maven build.

## Maven

Applications must add the following dependency to their pom.xml,

```
<dependency>
  <groupId>org.forgerock.commons</groupId>
  <artifactId>openapi-war-overlay</artifactId>
  <version>${project.version}</version>
  <type>war</type>
</dependency>
```

The WAR overlay is applied via an overlay entry in the `maven-war-plugin`,

```
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-war-plugin</artifactId>
      <executions>
        <execution>
          <id>war</id>
          <phase>package</phase>
          <goals>
            <goal>war</goal>
          </goals>
          <configuration>
            <classifier>servlet</classifier>
            <overlays>
              <overlay>
                <groupId>org.forgerock.commons</groupId>
                <artifactId>openapi-war-overlay</artifactId>
              </overlay>
            </overlays>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

## web.xml

For embedded Jetty servers, the following lines in `web.xml` will make the
assets available via HTTP,

```
<servlet>
    <servlet-name>OpenAPIAssetsServlet</servlet-name>
    <servlet-class>org.eclipse.jetty.servlet.DefaultServlet</servlet-class>
</servlet>

<servlet-mapping>
    <servlet-name>OpenAPIAssetsServlet</servlet-name>
    <url-pattern>/openapi/*</url-pattern>
</servlet-mapping>
```

## Swagger UI

The current version of [Swagger UI](https://github.com/swagger-api/swagger-ui) in this module is **2.1.4**.

We had to make minor modifications to `swagger-ui.js` in order to remove URL fragments prior
to executing HTTP requests. We changed,

```
Operation.prototype.urlify = function (args) {
  var formParams = {};
  var requestUrl = this.path;
```

to,

```
Operation.prototype.urlify = function (args) {
  var formParams = {};
  var requestUrl = this.path.replace(/#.*/, '');
```

## CSS Theme

The Swagger UI theme CSS file at,

```
/src/main/webapp/openapi/themes/theme-flattop.css
```

was downloaded from [github.com/ostranme/swagger-ui-themes](https://github.com/ostranme/swagger-ui-themes).