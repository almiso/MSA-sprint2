import { ApolloServer } from '@apollo/server';
import { startStandaloneServer } from '@apollo/server/standalone';
import { buildSubgraphSchema } from '@apollo/subgraph';
import gql from 'graphql-tag';

const typeDefs = gql`
  type Booking @key(fields: "id") {
    id: ID!
    userId: String!
    hotelId: String!
    promoCode: String
    discountPercent: Float
  }

  type Query {
    bookingsByUser(userId: String!): [Booking]
  }

`;

const MOCK_BOOKINGS = [
  { id: 'b1', userId: 'user1', hotelId: 'test-hotel-1', promoCode: 'SUMMER', discountPercent: 10 },
  { id: 'b2', userId: 'user1', hotelId: 'test-hotel-2', promoCode: 'WINTER', discountPercent: 20 },
  { id: 'b3', userId: 'user2', hotelId: 'test-hotel-1', promoCode: null, discountPercent: null },
];

const resolvers = {
  Query: {
    bookingsByUser: async (_, { userId }, { req }) => {
      const headerUserId = req.headers['userid'];
      if (!headerUserId || headerUserId !== userId) {
        throw new Error('Forbidden: Cannot access these bookings');
      }
      return MOCK_BOOKINGS.filter(b => b.userId === userId);
    },
  },
  Booking: {
    __resolveReference: ({ id }, { req }) => {
      const headerUserId = req.headers['userid'];
      const booking = MOCK_BOOKINGS.find(b => b.id === id);
      if (!headerUserId || (booking && booking.userId !== headerUserId)) {
        throw new Error('Forbidden');
      }
      return booking;
    }
  },
};

const server = new ApolloServer({
  schema: buildSubgraphSchema([{ typeDefs, resolvers }]),
});

startStandaloneServer(server, {
  listen: { port: 4001 },
  context: async ({ req }) => ({ req }),
}).then(() => {
  console.log('✅ Booking subgraph ready at http://localhost:4001/');
});
