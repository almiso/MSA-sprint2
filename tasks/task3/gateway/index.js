import { ApolloServer } from '@apollo/server';
import { startStandaloneServer } from '@apollo/server/standalone';
import { ApolloGateway, RemoteGraphQLDataSource } from '@apollo/gateway';

class AuthenticatedDataSource extends RemoteGraphQLDataSource {
  willSendRequest({ request, context }) {
    if (context.userid) {
      request.http.headers.set('userid', context.userid);
    }
  }
}

const gateway = new ApolloGateway({
  serviceList: [
    { name: 'booking', url: 'http://booking-subgraph:4001' },
    { name: 'hotel', url: 'http://hotel-subgraph:4002' },
    { name: 'promocode', url: 'http://promocode-subgraph:4003' }
  ],
  buildService({ name, url }) {
    return new AuthenticatedDataSource({ url });
  }
});

const server = new ApolloServer({ gateway, subscriptions: false });

startStandaloneServer(server, {
  listen: { port: 4000 },
  context: async ({ req }) => {
    return { userid: req.headers.userid || req.headers.userId || req.headers['userid'] };
  },
}).then(({ url }) => {
  console.log(`🚀 Gateway ready at ${url}`);
});
