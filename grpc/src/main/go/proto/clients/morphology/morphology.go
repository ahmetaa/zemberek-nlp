package morphology

import (
	"DragonDE/grpc"
	"DragonDE/grpc/proto/clients/morphology/pb"
	"golang.org/x/net/context"
	"log"
)

type AnalyzeSentenceResult struct {
	Text    string
	Results []*pb.SentenceWordAnalysisProto
}

func Call(text string) *AnalyzeSentenceResult {
	conn := grpcCaller.Init()
	defer grpcCaller.Close(conn)

	c := pb.NewMorphologyServiceClient(*conn)

	response, err := c.AnalyzeSentence(context.Background(), &pb.SentenceAnalysisRequest{Input: text})
	if err != nil {
		log.Fatalf("Error when calling SayHello: %s", err)
	}

	return &AnalyzeSentenceResult{Text: text, Results: response.Results}
}
