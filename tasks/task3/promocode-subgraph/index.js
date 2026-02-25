import { ApolloServer } from '@apollo/server';
import { startStandaloneServer } from '@apollo/server/standalone';
import { buildSubgraphSchema } from '@apollo/subgraph';
import gql from 'graphql-tag';

const typeDefs = gql`
  extend schema
    @link(url: "https://specs.apollo.dev/federation/v2.0", import: ["@key", "@external", "@requires", "@override"])

  extend type Booking @key(fields: "id") {
    id: ID! @external
    promoCode: String @external
    discountPercent: Float @override(from: "booking") @requires(fields: "promoCode")
    discountInfo: DiscountInfo @requires(fields: "promoCode")
  }

  type DiscountInfo {
    isValid: Boolean!
    originalDiscount: Float
    finalDiscount: Float
    description: String
  }

  type Query {
    activePromoCodes: [String!]!
  }
`;

const resolvers = {
    Query: {
        activePromoCodes: () => ['SUMMER', 'WINTER', 'VIP2023'],
    },
    Booking: {
        discountPercent: (booking) => {
            if (booking.promoCode === 'SUMMER') return 10.0;
            if (booking.promoCode === 'WINTER') return 20.0;
            return null;
        },
        discountInfo: (booking) => {
            let finalDiscount = null;
            let isValid = false;
            let description = 'No valid promo code applied';

            if (booking.promoCode === 'SUMMER') {
                finalDiscount = 10.0;
                isValid = true;
                description = 'Summer special 10% off';
            } else if (booking.promoCode === 'WINTER') {
                finalDiscount = 20.0;
                isValid = true;
                description = 'Winter special 20% off';
            } else if (booking.promoCode) {
                description = 'Invalid or expired promo code';
            }

            return {
                isValid,
                originalDiscount: null,
                finalDiscount,
                description
            };
        }
    },
};

const server = new ApolloServer({
    schema: buildSubgraphSchema([{ typeDefs, resolvers }]),
});

startStandaloneServer(server, {
    listen: { port: 4003 },
}).then(() => {
    console.log('✅ Promocode subgraph ready at http://localhost:4003/');
});
