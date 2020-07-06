package normalizer

import (
	"DragonDE/grpc"
	"DragonDE/grpc/proto/clients/normalization/pb"
	levenshtein "DragonDE/nlp"
	"golang.org/x/net/context"
	"log"
)

type NormalizationResult struct {
	Text               string
	NormalizedText     string
	DistanceLength     int
	DistancePercentage float32
}

func Call(text string) *NormalizationResult {
	conn := grpcCaller.Init()
	defer grpcCaller.Close(conn)

	c := pb.NewNormalizationServiceClient(*conn)

	response, err := c.Normalize(context.Background(), &pb.NormalizationRequest{Input: text})
	if err != nil {
		log.Fatalf("Error when calling SayHello: %s", err)
	}

	score := levenshtein.ComputeDistance(text, response.NormalizedInput)
	return &NormalizationResult{Text: text, NormalizedText: response.NormalizedInput, DistanceLength: score, DistancePercentage: float32((score * 100) / len(text))}
}
