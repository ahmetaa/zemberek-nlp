<?php
// GENERATED CODE -- DO NOT EDIT!

namespace Zemberek\Simple_analysis;

/**
 */
class SimpleAnalysisServiceClient extends \Grpc\BaseStub {

    /**
     * @param string $hostname hostname
     * @param array $opts channel options
     * @param \Grpc\Channel $channel (optional) re-use channel object
     */
    public function __construct($hostname, $opts, $channel = null) {
        parent::__construct($hostname, $opts, $channel);
    }

    /**
     * @param \Zemberek\Simple_analysis\SentenceRequest $argument input argument
     * @param array $metadata metadata
     * @param array $options call options
     */
    public function AnalyzeSentence(\Zemberek\Simple_analysis\SentenceRequest $argument,
      $metadata = [], $options = []) {
        return $this->_simpleRequest('/zemberek.simple_analysis.SimpleAnalysisService/AnalyzeSentence',
        $argument,
        ['\Zemberek\Simple_analysis\SentenceAnalysis_P', 'decode'],
        $metadata, $options);
    }

    /**
     * @param \Zemberek\Simple_analysis\WordRequest $argument input argument
     * @param array $metadata metadata
     * @param array $options call options
     */
    public function AnalyzeWord(\Zemberek\Simple_analysis\WordRequest $argument,
      $metadata = [], $options = []) {
        return $this->_simpleRequest('/zemberek.simple_analysis.SimpleAnalysisService/AnalyzeWord',
        $argument,
        ['\Zemberek\Simple_analysis\WordAnalysis_P', 'decode'],
        $metadata, $options);
    }

}
