package preprocess

import (
	"DragonDE/grpc"
	"DragonDE/grpc/proto/clients/preprocess/pb"
	"golang.org/x/net/context"
	"log"
)

type NormalizationResult struct {
	Text      string
	Sentences []string
}

func Call(text string, doNotSplitInDoubleQuotes bool) *NormalizationResult {
	conn := grpcCaller.Init()
	defer grpcCaller.Close(conn)

	c := pb.NewPreprocessingServiceClient(*conn)

	response, err := c.ExtractSentences(context.Background(), &pb.SentenceExtractionRequest{Document: text, DoNotSplitInDoubleQuotes: doNotSplitInDoubleQuotes})
	if err != nil {
		log.Fatalf("Error when calling SayHello: %s", err)
	}

	return &NormalizationResult{Text: text, Sentences: response.Sentences}
}
