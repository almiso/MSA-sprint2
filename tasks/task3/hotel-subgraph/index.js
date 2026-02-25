import { ApolloServer } from '@apollo/server';
import { startStandaloneServer } from '@apollo/server/standalone';
import { buildSubgraphSchema } from '@apollo/subgraph';
import gql from 'graphql-tag';
import DataLoader from 'dataloader';

const batchGetHotels = async (keys) => {
  console.log(`[DataLoader] Fetching hotels for keys: ${keys.join(', ')}`);

  // Fetch each hotel from the monolith REST API
  const promises = keys.map(async (key) => {
    try {
      const response = await fetch(`http://hotelio-monolith:8080/api/hotels/${key}`);
      if (!response.ok) {
        if (response.status === 404) return null;
        throw new Error(`Failed to fetch hotel ${key}: ${response.statusText}`);
      }
      return await response.json();
    } catch (error) {
      console.error(`Error fetching hotel ${key}:`, error);
      return null;
    }
  });

  return Promise.all(promises);
};

const typeDefs = gql`
  extend schema
    @link(url: "https://specs.apollo.dev/federation/v2.0", import: ["@key", "@external", "@requires"])

  type Hotel @key(fields: "id") {
    id: ID!
    name: String
    city: String
    stars: Int
  }

  extend type Booking @key(fields: "id") {
    id: ID! @external
    hotelId: String! @external
    hotel: Hotel @requires(fields: "hotelId")
  }

  type Query {
    hotelsByIds(ids: [ID!]!): [Hotel]
  }
`;

const resolvers = {
  Booking: {
    hotel: (booking, _, { dataLoaders }) => {
      return dataLoaders.hotelLoader.load(booking.hotelId);
    }
  },
  Hotel: {
    __resolveReference: async (reference, { dataLoaders }) => {
      return dataLoaders.hotelLoader.load(reference.id);
    },
  },
  Query: {
    hotelsByIds: async (_, { ids }, { dataLoaders }) => {
      return dataLoaders.hotelLoader.loadMany(ids);
    },
  },
};

const server = new ApolloServer({
  schema: buildSubgraphSchema([{ typeDefs, resolvers }]),
});

startStandaloneServer(server, {
  listen: { port: 4002 },
  context: async () => ({
    dataLoaders: {
      hotelLoader: new DataLoader(batchGetHotels),
    }
  }),
}).then(() => {
  console.log('✅ Hotel subgraph ready at http://localhost:4002/');
});
