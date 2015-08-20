To start the demo app.

First make sure it is built:

    mvn clean install

Then to get it running:

    mvn jetty:run -f forgerock-selfservice-example/pom.xml -Dmailserver.username=someusername -Dmailserver.password=somepassword -Duser.mail=some@email.here

It is necessary to give values to the above properties. There properties represent:

    mailserver.username - mail server username (use your forgerock email address)
    mailserver.password - mail server password (use your forgerock email password)
    user.mail - the email address for which to send password reset emails

Once the service is running you can visit this page:

    http://localhost:9999/example

To query all demo users run:

    curl --request GET \
      --url 'http://localhost:9999/example/selfservice/internal/users?_queryFilter=true' \
      --header 'content-type: application/json'