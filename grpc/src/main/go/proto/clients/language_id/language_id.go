package languageId

import (
	"DragonDE/grpc"
	"DragonDE/grpc/proto/clients/language_id/pb"
	"golang.org/x/net/context"
	"log"
)

func GetScores(text string, maxSampleCount int32) []*pb.IdResult {
	conn := grpcCaller.Init()
	defer grpcCaller.Close(conn)

	c := pb.NewLanguageIdServiceClient(*conn)

	response, err := c.DetectFast(context.Background(), &pb.LanguageIdRequest{Input: text, MaxSampleCount: maxSampleCount, IncludeScores: true, TrGroup: true})
	if err != nil {
		log.Fatalf("Error when calling SayHello: %s", err)
	}

	return response.IdResult
}
